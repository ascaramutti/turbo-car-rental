package com.turbo.document.service.impl;

import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.command.UploadDocumentCommand;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.repository.DriverRepository;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl - uploadDocument()")
class DocumentServiceUploadTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentServiceMapper documentServiceMapper;

    @InjectMocks private DocumentServiceImpl documentService;

    @Captor private ArgumentCaptor<Document> documentCaptor;

    // ── Helpers ──────────────────────────────────────────────────────

    private UploadDocumentCommand buildCommand(String documentType) {
        UploadDocumentCommand cmd = new UploadDocumentCommand();
        cmd.setFile(DocumentFixture.validPdf());
        cmd.setDocumentType(documentType);
        cmd.setUserId(DocumentFixture.DRIVER_USER_ID);
        return cmd;
    }

    private void mockDriverExists() {
        when(userRepository.findById(DocumentFixture.DRIVER_USER_ID))
                .thenReturn(Optional.of(DocumentFixture.testDriver()));
    }

    private void mockNoDuplicate() {
        when(documentRepository.existsByUserUserIdAndDocumentTypeAndStatusIn(
                eq(DocumentFixture.DRIVER_USER_ID), any(DocumentType.class), anyList()))
                .thenReturn(false);
    }

    private void mockFileStorage() {
        when(fileStorageService.store(any(), eq(DocumentFixture.DRIVER_USER_ID), anyString()))
                .thenReturn(DocumentFixture.STORED_FILE_URL);
    }

    private void mockMapperBuildsDocument() {
        when(documentServiceMapper.toNewDocument(any(), any(), anyString(), any()))
                .thenReturn(DocumentFixture.pendingLicense());
    }

    private void mockSaveReturnsDocument() {
        when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Happy paths ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Driver uploads valid PDF - saves document with PENDING status")
        void upload_validPdf_success() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            mockDriverExists();
            mockNoDuplicate();
            mockFileStorage();
            mockMapperBuildsDocument();
            mockSaveReturnsDocument();
            when(documentServiceMapper.toDocumentResponse(any(Document.class)))
                    .thenReturn(new DocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "DRIVERS_LICENSE", "license.pdf", 2048576L, "PENDING", null, null, null));

            DocumentResponse result = documentService.uploadDocument(command);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Driver uploads STUDY_PERMIT - accepted as valid type")
        void upload_studyPermit_success() {
            UploadDocumentCommand command = buildCommand("STUDY_PERMIT");
            mockDriverExists();
            mockNoDuplicate();
            mockFileStorage();
            mockMapperBuildsDocument();
            mockSaveReturnsDocument();
            when(documentServiceMapper.toDocumentResponse(any(Document.class))).thenReturn(
                    new DocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "STUDY_PERMIT", "permit.jpg", 1024L, "PENDING", null, null, null));

            DocumentResponse result = documentService.uploadDocument(command);

            assertThat(result).isNotNull();
        }
    }

    // ── Verifications ────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("File is stored before saving document to DB")
        void upload_storesFileBeforeSave() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            mockDriverExists();
            mockNoDuplicate();
            mockFileStorage();
            mockMapperBuildsDocument();
            mockSaveReturnsDocument();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            documentService.uploadDocument(command);

            var inOrder = inOrder(fileStorageService, documentRepository);
            inOrder.verify(fileStorageService).store(any(), eq(DocumentFixture.DRIVER_USER_ID), eq("DRIVERS_LICENSE"));
            inOrder.verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Mapper is called with correct arguments to build document")
        void upload_callsMapperWithCorrectArgs() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            mockDriverExists();
            mockNoDuplicate();
            mockFileStorage();
            mockMapperBuildsDocument();
            mockSaveReturnsDocument();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            documentService.uploadDocument(command);

            verify(documentServiceMapper).toNewDocument(any(User.class), eq(DocumentType.DRIVERS_LICENSE), eq(DocumentFixture.STORED_FILE_URL), any());
        }
    }

    // ── Unhappy paths ────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Non-driver user - throws DOC-011")
        void upload_notADriver_throwsNotADriver() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            Long ownerUserId = 20L;
            command.setUserId(ownerUserId);
            when(userRepository.findById(ownerUserId))
                    .thenReturn(Optional.of(DocumentFixture.testCarOwner()));

            assertThatThrownBy(() -> documentService.uploadDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.NOT_A_DRIVER);

            verifyNoInteractions(fileStorageService, documentServiceMapper);
        }

        @Test
        @DisplayName("Invalid file format (.docx) - throws DOC-001")
        void upload_invalidFormat_throwsInvalidFileFormat() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            command.setFile(DocumentFixture.invalidFormatFile());
            mockDriverExists();

            assertThatThrownBy(() -> documentService.uploadDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.INVALID_FILE_FORMAT);

            verifyNoInteractions(fileStorageService);
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("File exceeds 5MB - throws DOC-002")
        void upload_fileTooLarge_throwsFileTooLarge() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            command.setFile(DocumentFixture.oversizedFile());
            mockDriverExists();

            assertThatThrownBy(() -> documentService.uploadDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.FILE_TOO_LARGE);
        }

        @Test
        @DisplayName("Invalid document type 'INSURANCE' - throws DOC-003")
        void upload_invalidDocumentType_throwsInvalidDocumentType() {
            UploadDocumentCommand command = buildCommand("INSURANCE");
            mockDriverExists();

            assertThatThrownBy(() -> documentService.uploadDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.INVALID_DOCUMENT_TYPE);
        }

        @Test
        @DisplayName("Duplicate PENDING document exists - throws DOC-004")
        void upload_duplicatePending_throwsDocumentAlreadyExists() {
            UploadDocumentCommand command = buildCommand("DRIVERS_LICENSE");
            mockDriverExists();
            when(documentRepository.existsByUserUserIdAndDocumentTypeAndStatusIn(
                    eq(DocumentFixture.DRIVER_USER_ID), eq(DocumentType.DRIVERS_LICENSE),
                    eq(List.of(DocumentStatus.PENDING, DocumentStatus.APPROVED))))
                    .thenReturn(true);

            assertThatThrownBy(() -> documentService.uploadDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ALREADY_EXISTS);
        }
    }
}
