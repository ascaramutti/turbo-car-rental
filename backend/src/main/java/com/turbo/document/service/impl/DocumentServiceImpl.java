package com.turbo.document.service.impl;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.model.enums.LicenseClass;
import com.turbo.document.model.enums.ReviewAction;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.DocumentService;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.document.validation.DocumentValidationConstraints;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.repository.DriverRepository;
import com.turbo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final FileStorageService fileStorageService;
    private final DocumentServiceMapper documentServiceMapper;

    @Override
    @Transactional
    public Document uploadDocument(UploadDocumentCommand command) {
        User user = findUserById(command.getUserId());
        validateIsDriver(user);

        DocumentType documentType = parseDocumentType(command.getDocumentType());
        validateIsDriverDocumentType(documentType);
        validateFile(command.getFile());
        validateNoDuplicate(user.getUserId(), documentType);

        String fileUrl = fileStorageService.store(command.getFile(), user.getUserId(), documentType.name());
        Document document = documentServiceMapper.toNewDocument(user, documentType, fileUrl, command.getFile());

        return documentRepository.save(document);
    }

    @Override
    public List<Document> getMyDocuments(Long userId) {
        return documentRepository.findByUserUserId(userId);
    }

    @Override
    @Transactional
    public Document reuploadDocument(ReuploadDocumentCommand command) {
        Document document = findDocumentById(command.getDocumentId());
        validateOwnership(document, command.getUserId());
        validateReuploadAllowed(document);
        validateFile(command.getFile());

        fileStorageService.delete(document.getFileUrl());
        String newFileUrl = fileStorageService.store(command.getFile(), command.getUserId(), document.getDocumentType().name());

        documentServiceMapper.updateDocumentForReupload(newFileUrl, command.getFile(), document);
        return documentRepository.save(document);
    }

    @Override
    public List<Document> getPendingDocuments() {
        return documentRepository.findByStatus(DocumentStatus.PENDING);
    }

    @Override
    @Transactional
    public Document reviewDocument(ReviewDocumentCommand command) {
        Document document = findDocumentById(command.getDocumentId());
        validateDocumentIsPending(document);

        ReviewAction action = parseReviewAction(command.getAction());

        if (action == ReviewAction.APPROVE) {
            approveDocument(document, command);
        } else {
            rejectDocument(document, command);
        }

        documentServiceMapper.applyReviewMetadata(command.getAdminId(), document);
        return documentRepository.save(document);
    }

    @Override
    public List<Document> getDocumentsByUser(Long userId) {
        findUserById(userId);
        return documentRepository.findByUserUserId(userId);
    }

    @Override
    public Document getDocumentForDownload(Long documentId, Long userId) {
        Document document = findDocumentById(documentId);
        validateOwnership(document, userId);
        return document;
    }

    @Override
    public Document getDocumentForAdminView(Long documentId) {
        return findDocumentById(documentId);
    }

    // ── Validation helpers ─────────────────────────────────────────

    /** Finds a user by ID or throws if not found. */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    /** Validates that the user is a Driver. */
    private void validateIsDriver(User user) {
        if (!(user instanceof Driver)) {
            throw new BusinessException(DocumentErrorCode.NOT_A_DRIVER);
        }
    }

    /** Validates that the document type is valid for driver uploads (DRIVERS_LICENSE or STUDY_PERMIT). */
    private void validateIsDriverDocumentType(DocumentType documentType) {
        if (!DocumentValidationConstraints.DRIVER_DOCUMENT_TYPES.contains(documentType)) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_TYPE);
        }
    }

    /** Parses the document type string or throws if invalid. */
    private DocumentType parseDocumentType(String type) {
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_TYPE);
        }
    }

    /** Validates file format and size. */
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !DocumentValidationConstraints.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(DocumentErrorCode.INVALID_FILE_FORMAT);
        }
        if (file.getSize() > DocumentValidationConstraints.MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(DocumentErrorCode.FILE_TOO_LARGE);
        }
    }

    /** Throws if a PENDING or APPROVED document of the same type already exists. */
    private void validateNoDuplicate(Long userId, DocumentType documentType) {
        List<DocumentStatus> blockingStatuses = List.of(DocumentStatus.PENDING, DocumentStatus.APPROVED);
        if (documentRepository.existsByUserUserIdAndDocumentTypeAndStatusIn(userId, documentType, blockingStatuses)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ALREADY_EXISTS);
        }
    }

    /** Finds a document by ID or throws if not found. */
    private Document findDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    /** Validates that the document belongs to the requesting user. */
    private void validateOwnership(Document document, Long userId) {
        if (!document.getUser().getUserId().equals(userId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    /** Validates that the document can be re-uploaded (PENDING or REJECTED only). */
    private void validateReuploadAllowed(Document document) {
        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ALREADY_APPROVED);
        }
    }

    /** Validates that the document has PENDING status for review. */
    private void validateDocumentIsPending(Document document) {
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_PENDING);
        }
    }

    /** Parses the review action string. DTO @Pattern already validates, this is defense in depth. */
    private ReviewAction parseReviewAction(String action) {
        try {
            return ReviewAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_PENDING);
        }
    }

    // ── Business logic helpers ─────────────────────────────────────

    /** Approves a document and updates driver fields if applicable. */
    private void approveDocument(Document document, ReviewDocumentCommand command) {
        documentServiceMapper.applyApproval(document);

        if (document.getDocumentType() == DocumentType.DRIVERS_LICENSE) {
            approveLicense(document, command);
        }

        checkAndSetDriverVerification(document.getUser());
    }

    /** Sets license class and work eligibility on the driver entity. */
    private void approveLicense(Document document, ReviewDocumentCommand command) {
        if (command.getLicenseClass() == null || command.getLicenseClass().isBlank()) {
            throw new BusinessException(DocumentErrorCode.LICENSE_CLASS_REQUIRED);
        }

        LicenseClass licenseClass = parseLicenseClass(command.getLicenseClass());
        Driver driver = driverRepository.findById(document.getUser().getUserId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        documentServiceMapper.applyLicenseApproval(licenseClass, driver);
        driverRepository.save(driver);
    }

    /** Parses the license class string or throws if invalid. */
    private LicenseClass parseLicenseClass(String licenseClass) {
        try {
            return LicenseClass.valueOf(licenseClass.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(DocumentErrorCode.LICENSE_CLASS_REQUIRED);
        }
    }

    /**
     * Checks if all required documents are approved and sets isVerified on the driver.
     * Rule: DRIVERS_LICENSE must be APPROVED. STUDY_PERMIT must be APPROVED if it exists
     * (it's optional — if no STUDY_PERMIT was uploaded, it's not required).
     */
    private void checkAndSetDriverVerification(User user) {
        List<Document> userDocs = documentRepository.findByUserUserId(user.getUserId());

        boolean licenseApproved = userDocs.stream()
                .anyMatch(d -> d.getDocumentType() == DocumentType.DRIVERS_LICENSE && d.getStatus() == DocumentStatus.APPROVED);

        boolean studyPermitApproved = userDocs.stream()
                .noneMatch(d -> d.getDocumentType() == DocumentType.STUDY_PERMIT)
                || userDocs.stream()
                .anyMatch(d -> d.getDocumentType() == DocumentType.STUDY_PERMIT && d.getStatus() == DocumentStatus.APPROVED);

        if (licenseApproved && studyPermitApproved) {
            user.setIsVerified(true);
            userRepository.save(user);
        }
    }

    /** Rejects a document with the provided reason. */
    private void rejectDocument(Document document, ReviewDocumentCommand command) {
        if (command.getRejectionReason() == null || command.getRejectionReason().isBlank()) {
            throw new BusinessException(DocumentErrorCode.REJECTION_REASON_REQUIRED);
        }
        documentServiceMapper.applyRejection(command.getRejectionReason(), document);
    }
}
