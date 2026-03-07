package com.turbo.vehicle.service.impl;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.document.validation.DocumentValidationConstraints;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.model.User;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.VehicleDocumentService;
import com.turbo.vehicle.service.command.GetVehicleDocumentsCommand;
import com.turbo.vehicle.service.command.ReuploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.UploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class VehicleDocumentServiceImpl implements VehicleDocumentService {

    private final DocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final DocumentServiceMapper documentServiceMapper;

    @Override
    @Transactional
    public Document uploadVehicleDocument(UploadVehicleDocumentCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());

        DocumentType documentType = parseDocumentType(command.getDocumentType());
        validateIsVehicleDocumentType(documentType);
        validateFile(command.getFile());
        validateNoDuplicateVehicleDocument(command.getVehicleId(), documentType);

        User owner = findUserById(command.getOwnerId());
        String fileUrl = fileStorageService.store(command.getFile(), command.getOwnerId(), documentType.name());

        Document document = documentServiceMapper.toNewDocument(owner, documentType, fileUrl, command.getFile());
        document.setVehicleId(command.getVehicleId());

        return documentRepository.save(document);
    }

    @Override
    public List<Document> getVehicleDocuments(GetVehicleDocumentsCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());
        return documentRepository.findByVehicleId(command.getVehicleId());
    }

    @Override
    @Transactional
    public Document reuploadVehicleDocument(ReuploadVehicleDocumentCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());

        Document document = findDocumentById(command.getDocumentId());
        validateDocumentBelongsToVehicle(document, command.getVehicleId());
        validateReuploadAllowed(document);
        validateFile(command.getFile());

        fileStorageService.delete(document.getFileUrl());
        String newFileUrl = fileStorageService.store(command.getFile(), command.getOwnerId(), document.getDocumentType().name());

        documentServiceMapper.updateDocumentForReupload(newFileUrl, command.getFile(), document);
        return documentRepository.save(document);
    }

    @Override
    public Document getDocumentForView(ViewVehicleDocumentCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());

        Document document = findDocumentById(command.getDocumentId());
        validateDocumentBelongsToVehicle(document, command.getVehicleId());
        return document;
    }

    @Override
    public Document getDocumentForDownload(ViewVehicleDocumentCommand command) {
        return getDocumentForView(command);
    }

    /** Lookup helpers */

    /** Finds a vehicle by ID or throws if not found. */
    private Vehicle findVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));
    }

    /** Finds a document by ID or throws if not found. */
    private Document findDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));
    }

    /** Finds a user by ID or throws if not found. */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    /** Validation helpers */

    /** Validates that the vehicle belongs to the requesting user. */
    private void validateOwnership(Vehicle vehicle, Long ownerId) {
        if (!vehicle.getOwner().getUserId().equals(ownerId)) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_ACCESS_DENIED);
        }
    }

    /** Parses the document type string or throws if invalid for vehicle documents. */
    private DocumentType parseDocumentType(String type) {
        try {
            return DocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(VehicleErrorCode.INVALID_VEHICLE_DOCUMENT_TYPE);
        }
    }

    /** Validates that the document type is a valid vehicle document type. */
    private void validateIsVehicleDocumentType(DocumentType documentType) {
        if (!DocumentValidationConstraints.VEHICLE_DOCUMENT_TYPES.contains(documentType)) {
            throw new BusinessException(VehicleErrorCode.INVALID_VEHICLE_DOCUMENT_TYPE);
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

    /** Throws if a PENDING or APPROVED document of the same type already exists for this vehicle. */
    private void validateNoDuplicateVehicleDocument(Long vehicleId, DocumentType documentType) {
        List<DocumentStatus> blockingStatuses = List.of(DocumentStatus.PENDING, DocumentStatus.APPROVED);
        if (documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(vehicleId, documentType, blockingStatuses)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ALREADY_EXISTS);
        }
    }

    /** Validates that the document belongs to the specified vehicle. */
    private void validateDocumentBelongsToVehicle(Document document, Long vehicleId) {
        if (document.getVehicleId() == null || !document.getVehicleId().equals(vehicleId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    /** Validates that the document can be re-uploaded (not APPROVED). */
    private void validateReuploadAllowed(Document document) {
        if (document.getStatus() == DocumentStatus.APPROVED) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_ALREADY_APPROVED);
        }
    }
}
