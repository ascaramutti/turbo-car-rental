package com.turbo.document.service.impl;

import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.user.repository.DriverRepository;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentServiceImpl - query methods")
class DocumentServiceGetTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentServiceMapper documentServiceMapper;

    @InjectMocks private DocumentServiceImpl documentService;

    // ── getMyDocuments ───────────────────────────────────────────────

    @Nested
    @DisplayName("getMyDocuments")
    class GetMyDocuments {

        @Test
        @DisplayName("Returns list of user's documents")
        void getMyDocuments_returnsList() {
            List<Document> docs = List.of(DocumentFixture.pendingLicense());
            when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID)).thenReturn(docs);

            List<Document> result = documentService.getMyDocuments(DocumentFixture.DRIVER_USER_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Returns empty list when no documents exist")
        void getMyDocuments_noDocuments_returnsEmpty() {
            when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID)).thenReturn(Collections.emptyList());

            List<Document> result = documentService.getMyDocuments(DocumentFixture.DRIVER_USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // ── getPendingDocuments ──────────────────────────────────────────

    @Nested
    @DisplayName("getPendingDocuments")
    class GetPendingDocuments {

        @Test
        @DisplayName("Returns only PENDING documents")
        void getPendingDocuments_returnsPendingOnly() {
            List<Document> docs = List.of(DocumentFixture.pendingLicense());
            when(documentRepository.findByStatus(DocumentStatus.PENDING)).thenReturn(docs);

            List<Document> result = documentService.getPendingDocuments();

            assertThat(result).hasSize(1);
        }
    }

    // ── getDocumentsByUser ───────────────────────────────────────────

    @Nested
    @DisplayName("getDocumentsByUser")
    class GetDocumentsByUser {

        @Test
        @DisplayName("Returns documents for existing user")
        void getDocumentsByUser_success() {
            when(userRepository.findById(DocumentFixture.DRIVER_USER_ID))
                    .thenReturn(Optional.of(DocumentFixture.testDriver()));
            List<Document> docs = List.of(DocumentFixture.pendingLicense());
            when(documentRepository.findByUserUserId(DocumentFixture.DRIVER_USER_ID)).thenReturn(docs);

            List<Document> result = documentService.getDocumentsByUser(DocumentFixture.DRIVER_USER_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("User not found - throws AUTH-002")
        void getDocumentsByUser_userNotFound_throwsUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocumentsByUser(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        }
    }

    // ── getDocumentForDownload ───────────────────────────────────────

    @Nested
    @DisplayName("getDocumentForDownload")
    class GetDocumentForDownload {

        @Test
        @DisplayName("Returns document when owner matches")
        void getDocumentForDownload_ownerMatches_returnsDocument() {
            Document doc = DocumentFixture.pendingLicense();
            when(documentRepository.findById(DocumentFixture.DOCUMENT_ID)).thenReturn(Optional.of(doc));

            Document result = documentService.getDocumentForDownload(DocumentFixture.DOCUMENT_ID, DocumentFixture.DRIVER_USER_ID);

            assertThat(result).isEqualTo(doc);
        }

        @Test
        @DisplayName("Document not found - throws DOC-007")
        void getDocumentForDownload_notFound_throwsDocumentNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocumentForDownload(999L, DocumentFixture.DRIVER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Wrong owner - throws DOC-006")
        void getDocumentForDownload_wrongOwner_throwsAccessDenied() {
            Document doc = DocumentFixture.pendingLicense();
            when(documentRepository.findById(DocumentFixture.DOCUMENT_ID)).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> documentService.getDocumentForDownload(DocumentFixture.DOCUMENT_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    // ── getDocumentForAdminView ──────────────────────────────────────

    @Nested
    @DisplayName("getDocumentForAdminView")
    class GetDocumentForAdminView {

        @Test
        @DisplayName("Returns document for any admin")
        void getDocumentForAdminView_returnsDocument() {
            Document doc = DocumentFixture.pendingLicense();
            when(documentRepository.findById(DocumentFixture.DOCUMENT_ID)).thenReturn(Optional.of(doc));

            Document result = documentService.getDocumentForAdminView(DocumentFixture.DOCUMENT_ID);

            assertThat(result).isEqualTo(doc);
        }

        @Test
        @DisplayName("Document not found - throws DOC-007")
        void getDocumentForAdminView_notFound_throwsDocumentNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocumentForAdminView(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }
    }
}
