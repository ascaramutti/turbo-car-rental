package com.turbo.document.service.impl;

import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.DocumentErrorCode;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl - reuploadDocument()")
class DocumentServiceReuploadTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentServiceMapper documentServiceMapper;

    @InjectMocks private DocumentServiceImpl documentService;

    @Captor private ArgumentCaptor<Document> documentCaptor;

    private static final String NEW_FILE_URL = "uploads/10/drivers_license_new.pdf";

    // ── Helpers ──────────────────────────────────────────────────────

    /** Builds a ReuploadDocumentCommand with default test values. */
    private ReuploadDocumentCommand buildReuploadCommand(org.springframework.web.multipart.MultipartFile file, Long userId) {
        ReuploadDocumentCommand cmd = new ReuploadDocumentCommand();
        cmd.setDocumentId(DocumentFixture.DOCUMENT_ID);
        cmd.setFile(file);
        cmd.setUserId(userId);
        return cmd;
    }

    private void mockDocumentFound(Document document) {
        when(documentRepository.findById(DocumentFixture.DOCUMENT_ID)).thenReturn(Optional.of(document));
    }

    private void mockFileStorageForReupload() {
        when(fileStorageService.store(any(), eq(DocumentFixture.DRIVER_USER_ID), anyString()))
                .thenReturn(NEW_FILE_URL);
    }

    private void mockSave() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Happy paths ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Reupload REJECTED document - calls mapper to reset and saves")
        void reupload_rejectedDocument_success() {
            Document rejected = DocumentFixture.rejectedLicense();
            mockDocumentFound(rejected);
            mockFileStorageForReupload();
            mockSave();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.validPdf(), DocumentFixture.DRIVER_USER_ID));

            verify(documentServiceMapper).updateDocumentForReupload(eq(NEW_FILE_URL), any(), eq(rejected));
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Reupload PENDING document - allowed (user corrects mistake)")
        void reupload_pendingDocument_success() {
            Document pending = DocumentFixture.pendingLicense();
            mockDocumentFound(pending);
            mockFileStorageForReupload();
            mockSave();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.validPdf(), DocumentFixture.DRIVER_USER_ID));

            verify(documentRepository).save(any(Document.class));
        }
    }

    // ── Verifications ────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("Old file is deleted before storing the new one")
        void reupload_deletesOldFileBeforeStoringNew() {
            Document rejected = DocumentFixture.rejectedLicense();
            mockDocumentFound(rejected);
            mockFileStorageForReupload();
            mockSave();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.validPdf(), DocumentFixture.DRIVER_USER_ID));

            var inOrder = inOrder(fileStorageService);
            inOrder.verify(fileStorageService).delete(DocumentFixture.STORED_FILE_URL);
            inOrder.verify(fileStorageService).store(any(), eq(DocumentFixture.DRIVER_USER_ID), anyString());
        }

        @Test
        @DisplayName("Mapper receives the new file URL and file for metadata update")
        void reupload_mapperReceivesCorrectArgs() {
            Document rejected = DocumentFixture.rejectedLicense();
            mockDocumentFound(rejected);
            mockFileStorageForReupload();
            mockSave();
            when(documentServiceMapper.toDocumentResponse(any())).thenReturn(mock(DocumentResponse.class));

            MockMultipartFile newFile = DocumentFixture.validJpg();
            documentService.reuploadDocument(buildReuploadCommand(newFile, DocumentFixture.DRIVER_USER_ID));

            verify(documentServiceMapper).updateDocumentForReupload(eq(NEW_FILE_URL), eq(newFile), eq(rejected));
        }
    }

    // ── Unhappy paths ────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Document not found - throws DOC-007")
        void reupload_notFound_throwsDocumentNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());
            ReuploadDocumentCommand cmd = buildReuploadCommand(DocumentFixture.validPdf(), DocumentFixture.DRIVER_USER_ID);
            cmd.setDocumentId(999L);

            assertThatThrownBy(() -> documentService.reuploadDocument(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Document belongs to another user - throws DOC-006")
        void reupload_notOwner_throwsAccessDenied() {
            Document doc = DocumentFixture.pendingLicense();
            mockDocumentFound(doc);

            assertThatThrownBy(() -> documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.validPdf(), 999L)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Document already APPROVED - throws DOC-005")
        void reupload_approved_throwsAlreadyApproved() {
            Document approved = DocumentFixture.approvedLicense();
            mockDocumentFound(approved);

            assertThatThrownBy(() -> documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.validPdf(), DocumentFixture.DRIVER_USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ALREADY_APPROVED);
        }

        @Test
        @DisplayName("New file has invalid format - throws DOC-001")
        void reupload_invalidFormat_throwsInvalidFileFormat() {
            Document rejected = DocumentFixture.rejectedLicense();
            mockDocumentFound(rejected);

            assertThatThrownBy(() -> documentService.reuploadDocument(buildReuploadCommand(DocumentFixture.invalidFormatFile(), DocumentFixture.DRIVER_USER_ID)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.INVALID_FILE_FORMAT);

            verifyNoInteractions(fileStorageService);
        }
    }
}
