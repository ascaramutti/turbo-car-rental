package com.turbo.document.service.impl;

import com.turbo.document.dto.AdminDocumentResponse;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.model.enums.LicenseClass;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.user.model.Driver;
import com.turbo.user.repository.DriverRepository;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl - reviewDocument()")
class DocumentServiceReviewTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentServiceMapper documentServiceMapper;

    @InjectMocks private DocumentServiceImpl documentService;


    // ── Helpers ──────────────────────────────────────────────────────

    private void mockDocumentFound(Document document) {
        when(documentRepository.findById(DocumentFixture.DOCUMENT_ID)).thenReturn(Optional.of(document));
    }

    private void mockDriverFound() {
        when(driverRepository.findById(DocumentFixture.DRIVER_USER_ID))
                .thenReturn(Optional.of(DocumentFixture.testDriver()));
    }

    private void mockSave() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void mockMapperReturnsAdminResponse() {
        when(documentServiceMapper.toAdminDocumentResponse(any(Document.class)))
                .thenReturn(mock(AdminDocumentResponse.class));
    }

    /** Mocks the verification check: returns docs for the driver's userId. */
    private void mockUserDocsForVerification(List<Document> docs) {
        when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID)).thenReturn(docs);
    }

    // ── Happy paths ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Approve DRIVERS_LICENSE with CLASS_4 - calls mapper with CLASS_4 and saves driver")
        void review_approveLicenseClass4_success() {
            Document doc = DocumentFixture.pendingLicense();
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass4Request());
            mockDocumentFound(doc);
            mockDriverFound();
            mockSave();
            mockUserDocsForVerification(List.of(doc));
            mockMapperReturnsAdminResponse();

            documentService.reviewDocument(command);

            verify(documentServiceMapper).applyApproval(doc);
            verify(documentServiceMapper).applyLicenseApproval(eq(LicenseClass.CLASS_4), any(Driver.class));
            verify(driverRepository).save(any(Driver.class));
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("Approve DRIVERS_LICENSE with CLASS_5 - calls mapper with CLASS_5")
        void review_approveLicenseClass5_success() {
            Document doc = DocumentFixture.pendingLicense();
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass5Request());
            mockDocumentFound(doc);
            mockDriverFound();
            mockSave();
            mockUserDocsForVerification(List.of(doc));
            mockMapperReturnsAdminResponse();

            documentService.reviewDocument(command);

            verify(documentServiceMapper).applyLicenseApproval(eq(LicenseClass.CLASS_5), any(Driver.class));
            verify(driverRepository).save(any(Driver.class));
        }

        @Test
        @DisplayName("Approve STUDY_PERMIT - no license class needed")
        void review_approveStudyPermit_success() {
            Document doc = DocumentFixture.pendingStudyPermit();
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approvePermitRequest());
            mockDocumentFound(doc);
            mockSave();
            mockUserDocsForVerification(List.of(doc));
            mockMapperReturnsAdminResponse();

            documentService.reviewDocument(command);

            verify(documentServiceMapper).applyApproval(doc);
            verifyNoInteractions(driverRepository);
            verify(documentRepository).save(doc);
        }

        @Test
        @DisplayName("Reject document - sets status and rejection reason")
        void review_reject_setsReasonAndStatus() {
            Document doc = DocumentFixture.pendingLicense();
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.rejectRequest());
            mockDocumentFound(doc);
            mockSave();
            mockMapperReturnsAdminResponse();

            documentService.reviewDocument(command);

            verify(documentServiceMapper).applyRejection(eq("Document is blurry and unreadable. Please upload a clearer photo."), eq(doc));
            verify(documentRepository).save(doc);
        }
    }

    // ── Verification rule ────────────────────────────────────────────

    @Nested
    @DisplayName("Driver verification rule")
    class VerificationRule {

        @Test
        @DisplayName("All docs approved - Driver.isVerified set to true")
        void review_allDocsApproved_driverVerified() {
            Document license = DocumentFixture.pendingLicense();
            Document approvedPermit = DocumentFixture.approvedLicense();
            approvedPermit.setDocumentType(DocumentType.STUDY_PERMIT);
            approvedPermit.setDocumentId(200L);

            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass4Request());
            mockDocumentFound(license);
            mockDriverFound();
            mockSave();
            mockMapperReturnsAdminResponse();

            // After approval, checkAndSetDriverVerification queries all user docs.
            // The license will have been set to APPROVED by approveDocument() in-memory.
            // The permit is already APPROVED from fixture.
            when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID))
                    .thenAnswer(inv -> {
                        license.setStatus(DocumentStatus.APPROVED);
                        return List.of(license, approvedPermit);
                    });

            documentService.reviewDocument(command);

            verify(userRepository).save(any());
        }

        @Test
        @DisplayName("License approved but study permit still pending - Driver stays unverified")
        void review_licenseApprovedPermitPending_driverNotVerified() {
            Document license = DocumentFixture.pendingLicense();
            Document pendingPermit = DocumentFixture.pendingStudyPermit();
            pendingPermit.setDocumentId(200L);

            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass4Request());
            mockDocumentFound(license);
            mockDriverFound();
            mockSave();
            mockMapperReturnsAdminResponse();

            when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID))
                    .thenAnswer(inv -> {
                        license.setStatus(DocumentStatus.APPROVED);
                        return List.of(license, pendingPermit);
                    });

            documentService.reviewDocument(command);

            verify(userRepository, never()).save(any());
        }
    }

    // ── Unhappy paths ────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Document not found - throws DOC-007")
        void review_notFound_throwsDocumentNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass4Request());
            command.setDocumentId(999L);

            assertThatThrownBy(() -> documentService.reviewDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Document not PENDING - throws DOC-010")
        void review_alreadyApproved_throwsNotPending() {
            Document approved = DocumentFixture.approvedLicense();
            mockDocumentFound(approved);
            ReviewDocumentCommand command = DocumentFixture.toCommand(DocumentFixture.approveClass4Request());

            assertThatThrownBy(() -> documentService.reviewDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_PENDING);
        }

        @Test
        @DisplayName("Approve DRIVERS_LICENSE without licenseClass - throws DOC-009")
        void review_approveLicenseNoClass_throwsLicenseClassRequired() {
            Document doc = DocumentFixture.pendingLicense();
            mockDocumentFound(doc);
            ReviewDocumentCommand command = new ReviewDocumentCommand();
            command.setDocumentId(DocumentFixture.DOCUMENT_ID);
            command.setAction("APPROVE");
            command.setAdminId(DocumentFixture.ADMIN_USER_ID);

            assertThatThrownBy(() -> documentService.reviewDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.LICENSE_CLASS_REQUIRED);
        }

        @Test
        @DisplayName("Reject without reason - throws DOC-008")
        void review_rejectNoReason_throwsRejectionReasonRequired() {
            Document doc = DocumentFixture.pendingLicense();
            mockDocumentFound(doc);
            ReviewDocumentCommand command = new ReviewDocumentCommand();
            command.setDocumentId(DocumentFixture.DOCUMENT_ID);
            command.setAction("REJECT");
            command.setAdminId(DocumentFixture.ADMIN_USER_ID);

            assertThatThrownBy(() -> documentService.reviewDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.REJECTION_REASON_REQUIRED);
        }
    }
}
