package com.turbo.document.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.document.controller.mapper.DocumentControllerMapper;
import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.service.DocumentService;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.DocumentErrorCode;
import org.springframework.core.io.ByteArrayResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentController")
class DocumentControllerTest {

    @Mock private DocumentService documentService;
    @Mock private DocumentControllerMapper controllerMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private DocumentController documentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(securityHelper.getCurrentUser()).thenReturn(DocumentFixture.testDriver());
    }

    // ── POST /api/driver/documents/upload ────────────────────────────

    @Nested
    @DisplayName("POST /api/driver/documents/upload")
    class Upload {

        @Test
        @DisplayName("Valid upload - returns 200 with DocumentResponse")
        void upload_validRequest_returns200() throws Exception {
            MockMultipartFile file = DocumentFixture.validPdf();
            UploadDocumentCommand command = new UploadDocumentCommand();
            DocumentResponse response = new DocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "DRIVERS_LICENSE", "license.pdf", 2048576L, "PENDING", null, null, null);

            when(controllerMapper.toUploadCommand(any(), anyString(), anyLong())).thenReturn(command);
            when(documentService.uploadDocument(command)).thenReturn(response);

            mockMvc.perform(multipart("/api/driver/documents/upload")
                            .file(file)
                            .param("documentType", "DRIVERS_LICENSE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.documentType").value("DRIVERS_LICENSE"));

            verify(controllerMapper).toUploadCommand(any(), anyString(), anyLong());
        }
    }

    // ── GET /api/driver/documents/my ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/driver/documents/my")
    class GetMyDocuments {

        @Test
        @DisplayName("Returns 200 with list of documents")
        void getMyDocuments_returns200() throws Exception {
            DocumentResponse doc = new DocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "DRIVERS_LICENSE", "license.pdf", 2048576L, "PENDING", null, null, null);
            when(documentService.getMyDocuments(DocumentFixture.DRIVER_USER_ID)).thenReturn(List.of(doc));

            mockMvc.perform(get("/api/driver/documents/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].documentType").value("DRIVERS_LICENSE"));
        }

        @Test
        @DisplayName("Returns 200 with empty list when no documents")
        void getMyDocuments_empty_returns200() throws Exception {
            when(documentService.getMyDocuments(DocumentFixture.DRIVER_USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/driver/documents/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── PUT /api/driver/documents/{id}/reupload ──────────────────────

    @Nested
    @DisplayName("PUT /api/driver/documents/{id}/reupload")
    class Reupload {

        @Test
        @DisplayName("Valid reupload - returns 200")
        void reupload_validRequest_returns200() throws Exception {
            MockMultipartFile file = DocumentFixture.validPdf();
            ReuploadDocumentCommand command = new ReuploadDocumentCommand();
            DocumentResponse response = new DocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "DRIVERS_LICENSE", "new-license.pdf", 2048576L, "PENDING", null, null, null);

            when(controllerMapper.toReuploadCommand(anyLong(), any(), anyLong())).thenReturn(command);
            when(documentService.reuploadDocument(command)).thenReturn(response);

            mockMvc.perform(multipart("/api/driver/documents/100/reupload")
                            .file(file)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(controllerMapper).toReuploadCommand(anyLong(), any(), anyLong());
        }
    }

    // ── GET /api/driver/documents/{id}/view ──────────────────────────

    @Nested
    @DisplayName("GET /api/driver/documents/{id}/view")
    class ViewFile {

        @Test
        @DisplayName("Valid file download - returns 200 with file content")
        void downloadFile_validRequest_returns200() throws Exception {
            Document doc = DocumentFixture.pendingLicense();
            ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

            when(documentService.getDocumentForDownload(100L, DocumentFixture.DRIVER_USER_ID)).thenReturn(doc);
            when(fileStorageService.load(doc.getFileUrl())).thenReturn(resource);

            mockMvc.perform(get("/api/driver/documents/100/view"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Document not found - returns 404 with DOC-007")
        void downloadFile_notFound_returns404() throws Exception {
            when(documentService.getDocumentForDownload(999L, DocumentFixture.DRIVER_USER_ID))
                    .thenThrow(new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));

            mockMvc.perform(get("/api/driver/documents/999/view"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DOC-007"));
        }

        @Test
        @DisplayName("Wrong owner - returns 403 with DOC-006")
        void downloadFile_wrongOwner_returns403() throws Exception {
            when(documentService.getDocumentForDownload(100L, DocumentFixture.DRIVER_USER_ID))
                    .thenThrow(new BusinessException(DocumentErrorCode.DOCUMENT_ACCESS_DENIED));

            mockMvc.perform(get("/api/driver/documents/100/view"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("DOC-006"));
        }
    }

    // ── GET /api/driver/documents/{id}/download ────────────────────

    @Nested
    @DisplayName("GET /api/driver/documents/{id}/download")
    class DownloadFile {

        @Test
        @DisplayName("Valid file download - returns 200 with octet-stream")
        void downloadFile_validRequest_returns200() throws Exception {
            Document doc = DocumentFixture.pendingLicense();
            ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

            when(documentService.getDocumentForDownload(100L, DocumentFixture.DRIVER_USER_ID)).thenReturn(doc);
            when(fileStorageService.load(doc.getFileUrl())).thenReturn(resource);

            mockMvc.perform(get("/api/driver/documents/100/download"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Document not found - returns 404 with DOC-007")
        void downloadFile_notFound_returns404() throws Exception {
            when(documentService.getDocumentForDownload(999L, DocumentFixture.DRIVER_USER_ID))
                    .thenThrow(new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));

            mockMvc.perform(get("/api/driver/documents/999/download"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("DOC-007"));
        }

        @Test
        @DisplayName("Wrong owner - returns 403 with DOC-006")
        void downloadFile_wrongOwner_returns403() throws Exception {
            when(documentService.getDocumentForDownload(100L, DocumentFixture.DRIVER_USER_ID))
                    .thenThrow(new BusinessException(DocumentErrorCode.DOCUMENT_ACCESS_DENIED));

            mockMvc.perform(get("/api/driver/documents/100/download"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("DOC-006"));
        }
    }
}
