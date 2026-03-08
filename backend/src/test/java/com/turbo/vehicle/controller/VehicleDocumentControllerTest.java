package com.turbo.vehicle.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.model.Document;
import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.service.FileStorageService;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.vehicle.controller.mapper.VehicleDocumentControllerMapper;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.service.VehicleDocumentService;
import com.turbo.vehicle.service.command.GetVehicleDocumentsCommand;
import com.turbo.vehicle.service.command.ReuploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.UploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleDocumentController")
class VehicleDocumentControllerTest {

    @Mock private VehicleDocumentService vehicleDocumentService;
    @Mock private VehicleDocumentControllerMapper controllerMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private VehicleDocumentController vehicleDocumentController;

    private MockMvc mockMvc;

    private static final Long VEHICLE_ID = VehicleFixture.VEHICLE_ID;
    private static final Long DOCUMENT_ID = DocumentFixture.DOCUMENT_ID;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleDocumentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        lenient().when(securityHelper.getCurrentUser()).thenReturn(VehicleFixture.testCarOwner());
    }

    /** POST /api/owner/vehicles/{vehicleId}/documents/upload */

    @Nested
    @DisplayName("POST /api/owner/vehicles/{vehicleId}/documents/upload")
    class UploadDocument {

        @Test
        @DisplayName("Valid upload - returns 200 with DocumentResponse")
        void uploadDocument_validRequest_returns200() throws Exception {
            MockMultipartFile file = DocumentFixture.validPdf();
            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            Document document = new Document();
            document.setDocumentId(DOCUMENT_ID);
            DocumentResponse response = new DocumentResponse(
                    DOCUMENT_ID, VehicleFixture.OWNER_USER_ID, "INSURANCE",
                    "license.pdf", 2048576L, "PENDING", null, null, null
            );

            when(controllerMapper.toUploadCommand(eq(VEHICLE_ID), eq(VehicleFixture.OWNER_USER_ID), any(), eq("INSURANCE")))
                    .thenReturn(command);
            when(vehicleDocumentService.uploadVehicleDocument(command)).thenReturn(document);
            when(controllerMapper.toDocumentResponse(document)).thenReturn(response);

            mockMvc.perform(multipart("/api/owner/vehicles/{vehicleId}/documents/upload", VEHICLE_ID)
                            .file(file)
                            .param("documentType", "INSURANCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentId").value(DOCUMENT_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    /** GET /api/owner/vehicles/{vehicleId}/documents */

    @Nested
    @DisplayName("GET /api/owner/vehicles/{vehicleId}/documents")
    class GetDocuments {

        @Test
        @DisplayName("Returns 200 with list of documents")
        void getDocuments_returns200() throws Exception {
            Document document = new Document();
            document.setDocumentId(DOCUMENT_ID);
            DocumentResponse response = new DocumentResponse(
                    DOCUMENT_ID, VehicleFixture.OWNER_USER_ID, "INSURANCE",
                    "insurance.pdf", 2048576L, "PENDING", null, null, null
            );
            GetVehicleDocumentsCommand command = new GetVehicleDocumentsCommand();
            when(controllerMapper.toGetDocumentsCommand(VEHICLE_ID, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);
            when(vehicleDocumentService.getVehicleDocuments(command))
                    .thenReturn(List.of(document));
            when(controllerMapper.toDocumentResponseList(List.of(document)))
                    .thenReturn(List.of(response));

            mockMvc.perform(get("/api/owner/vehicles/{vehicleId}/documents", VEHICLE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].documentId").value(DOCUMENT_ID));
        }
    }

    /** GET /api/owner/vehicles/{vehicleId}/documents/{docId}/view */

    @Nested
    @DisplayName("GET /api/owner/vehicles/{vehicleId}/documents/{docId}/view")
    class ViewDocument {

        @Test
        @DisplayName("Returns 200 with file content for inline preview")
        void viewDocument_returns200() throws Exception {
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(VEHICLE_ID);
            Resource resource = new ByteArrayResource("pdf-content".getBytes());

            ViewVehicleDocumentCommand command = new ViewVehicleDocumentCommand();
            when(controllerMapper.toViewDocumentCommand(DOCUMENT_ID, VEHICLE_ID, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);
            when(vehicleDocumentService.getDocumentForView(command))
                    .thenReturn(document);
            when(fileStorageService.load(document.getFileUrl())).thenReturn(resource);

            mockMvc.perform(get("/api/owner/vehicles/{vehicleId}/documents/{documentId}/view",
                            VEHICLE_ID, DOCUMENT_ID))
                    .andExpect(status().isOk());
        }
    }

    /** GET /api/owner/vehicles/{vehicleId}/documents/{docId}/download */

    @Nested
    @DisplayName("GET /api/owner/vehicles/{vehicleId}/documents/{docId}/download")
    class DownloadDocument {

        @Test
        @DisplayName("Returns 200 with file content for download")
        void downloadDocument_returns200() throws Exception {
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(VEHICLE_ID);
            Resource resource = new ByteArrayResource("pdf-content".getBytes());

            ViewVehicleDocumentCommand command = new ViewVehicleDocumentCommand();
            when(controllerMapper.toViewDocumentCommand(DOCUMENT_ID, VEHICLE_ID, VehicleFixture.OWNER_USER_ID))
                    .thenReturn(command);
            when(vehicleDocumentService.getDocumentForDownload(command))
                    .thenReturn(document);
            when(fileStorageService.load(document.getFileUrl())).thenReturn(resource);

            mockMvc.perform(get("/api/owner/vehicles/{vehicleId}/documents/{documentId}/download",
                            VEHICLE_ID, DOCUMENT_ID))
                    .andExpect(status().isOk());
        }
    }

    /** PUT /api/owner/vehicles/{vehicleId}/documents/{docId}/reupload */

    @Nested
    @DisplayName("PUT /api/owner/vehicles/{vehicleId}/documents/{docId}/reupload")
    class ReuploadDocument {

        @Test
        @DisplayName("Valid reupload - returns 200 with DocumentResponse")
        void reuploadDocument_validRequest_returns200() throws Exception {
            MockMultipartFile file = DocumentFixture.validPdf();
            ReuploadVehicleDocumentCommand command = new ReuploadVehicleDocumentCommand();
            Document document = new Document();
            document.setDocumentId(DOCUMENT_ID);
            DocumentResponse response = new DocumentResponse(
                    DOCUMENT_ID, VehicleFixture.OWNER_USER_ID, "INSURANCE",
                    "license.pdf", 2048576L, "PENDING", null, null, null
            );

            when(controllerMapper.toReuploadCommand(eq(DOCUMENT_ID), eq(VEHICLE_ID), eq(VehicleFixture.OWNER_USER_ID), any()))
                    .thenReturn(command);
            when(vehicleDocumentService.reuploadVehicleDocument(command)).thenReturn(document);
            when(controllerMapper.toDocumentResponse(document)).thenReturn(response);

            mockMvc.perform(multipart("/api/owner/vehicles/{vehicleId}/documents/{documentId}/reupload",
                            VEHICLE_ID, DOCUMENT_ID)
                            .file(file)
                            .with(request -> { request.setMethod("PUT"); return request; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documentId").value(DOCUMENT_ID))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }
}
