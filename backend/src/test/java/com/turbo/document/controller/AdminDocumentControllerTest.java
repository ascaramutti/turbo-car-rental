package com.turbo.document.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbo.config.SecurityHelper;
import com.turbo.document.controller.mapper.AdminDocumentControllerMapper;
import com.turbo.document.dto.AdminDocumentResponse;
import com.turbo.document.dto.ReviewDocumentRequest;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.service.DocumentService;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDocumentController")
class AdminDocumentControllerTest {

    @Mock private DocumentService documentService;
    @Mock private AdminDocumentControllerMapper controllerMapper;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private AdminDocumentController adminDocumentController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDocumentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── GET /api/admin/documents/pending ─────────────────────────────

    @Nested
    @DisplayName("GET /api/admin/documents/pending")
    class GetPending {

        @Test
        @DisplayName("Returns 200 with list of pending documents")
        void getPending_returns200() throws Exception {
            AdminDocumentResponse doc = new AdminDocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "John Doe", "DRIVER", "DRIVERS_LICENSE", "license.pdf", 2048576L, "PENDING", null, null, null, null);
            when(documentService.getPendingDocuments()).thenReturn(List.of(doc));

            mockMvc.perform(get("/api/admin/documents/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[0].userFullName").value("John Doe"));
        }

        @Test
        @DisplayName("Returns 200 with empty list when no pending")
        void getPending_empty_returns200() throws Exception {
            when(documentService.getPendingDocuments()).thenReturn(List.of());

            mockMvc.perform(get("/api/admin/documents/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ── PUT /api/admin/documents/{id}/review ─────────────────────────

    @Nested
    @DisplayName("PUT /api/admin/documents/{id}/review")
    class Review {

        @Test
        @DisplayName("Approve with CLASS_4 - returns 200")
        void review_approveClass4_returns200() throws Exception {
            ReviewDocumentRequest request = DocumentFixture.approveClass4Request();
            ReviewDocumentCommand command = DocumentFixture.toCommand(request);
            AdminDocumentResponse response = new AdminDocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "John Doe", "DRIVER", "DRIVERS_LICENSE", "license.pdf", 2048576L, "APPROVED", null, null, DocumentFixture.ADMIN_USER_ID, null);

            when(securityHelper.getCurrentUser()).thenReturn(DocumentFixture.testAdmin());
            when(controllerMapper.toReviewCommand(anyLong(), any(ReviewDocumentRequest.class), anyLong())).thenReturn(command);
            when(documentService.reviewDocument(command)).thenReturn(response);

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            verify(controllerMapper).toReviewCommand(anyLong(), any(ReviewDocumentRequest.class), anyLong());
        }

        @Test
        @DisplayName("Reject with reason - returns 200")
        void review_reject_returns200() throws Exception {
            ReviewDocumentRequest request = DocumentFixture.rejectRequest();
            ReviewDocumentCommand command = DocumentFixture.toCommand(request);
            AdminDocumentResponse response = new AdminDocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "John Doe", "DRIVER", "DRIVERS_LICENSE", "license.pdf", 2048576L, "REJECTED", null, null, DocumentFixture.ADMIN_USER_ID, "Document is blurry");

            when(securityHelper.getCurrentUser()).thenReturn(DocumentFixture.testAdmin());
            when(controllerMapper.toReviewCommand(anyLong(), any(ReviewDocumentRequest.class), anyLong())).thenReturn(command);
            when(documentService.reviewDocument(command)).thenReturn(response);

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("Missing action - returns 400 validation error")
        void review_missingAction_returns400() throws Exception {
            ReviewDocumentRequest request = new ReviewDocumentRequest();

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Invalid action pattern - returns 400 validation error")
        void review_invalidAction_returns400() throws Exception {
            ReviewDocumentRequest request = new ReviewDocumentRequest();
            request.setAction("DROP TABLE");

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Invalid licenseClass pattern - returns 400 validation error")
        void review_invalidLicenseClass_returns400() throws Exception {
            ReviewDocumentRequest request = new ReviewDocumentRequest();
            request.setAction("APPROVE");
            request.setLicenseClass("'; DROP TABLE;--");

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Rejection reason with injection attempt - returns 400 validation error")
        void review_rejectionReasonInjection_returns400() throws Exception {
            ReviewDocumentRequest request = new ReviewDocumentRequest();
            request.setAction("REJECT");
            request.setRejectionReason("'; DROP TABLE users;--");

            mockMvc.perform(put("/api/admin/documents/100/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }

    // ── GET /api/admin/documents/user/{userId} ───────────────────────

    @Nested
    @DisplayName("GET /api/admin/documents/user/{userId}")
    class GetByUser {

        @Test
        @DisplayName("Returns 200 with user's documents")
        void getByUser_returns200() throws Exception {
            AdminDocumentResponse doc = new AdminDocumentResponse(1L, DocumentFixture.DRIVER_USER_ID, "John Doe", "DRIVER", "DRIVERS_LICENSE", "license.pdf", 2048576L, "PENDING", null, null, null, null);
            when(documentService.getDocumentsByUser(DocumentFixture.DRIVER_USER_ID)).thenReturn(List.of(doc));

            mockMvc.perform(get("/api/admin/documents/user/" + DocumentFixture.DRIVER_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userFullName").value("John Doe"));
        }
    }
}
