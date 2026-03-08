package com.turbo.vehicle.service.impl;

import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.service.mapper.DocumentServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.command.GetVehicleDocumentsCommand;
import com.turbo.vehicle.service.command.ReuploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.UploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleDocumentServiceImpl")
class VehicleDocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentServiceMapper documentServiceMapper;

    @InjectMocks private VehicleDocumentServiceImpl vehicleDocumentService;

    private static final Long VEHICLE_ID = VehicleFixture.VEHICLE_ID;
    private static final Long OWNER_ID = VehicleFixture.OWNER_USER_ID;
    private static final Long DOCUMENT_ID = DocumentFixture.DOCUMENT_ID;

    // ── uploadVehicleDocument ───────────────────────────────────────

    @Nested
    @DisplayName("uploadVehicleDocument()")
    class UploadVehicleDocument {

        @Test
        @DisplayName("Happy path - uploads vehicle document successfully")
        void upload_validRequest_returnsDocument() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.validPdf();

            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);
            command.setDocumentType("INSURANCE");

            Document savedDocument = new Document();
            savedDocument.setDocumentId(DOCUMENT_ID);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(
                    eq(VEHICLE_ID), eq(DocumentType.INSURANCE), anyList())).thenReturn(false);
            when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(VehicleFixture.testCarOwner()));
            when(fileStorageService.store(file, OWNER_ID, "INSURANCE")).thenReturn("uploads/20/insurance_uuid.pdf");
            when(documentServiceMapper.toNewDocument(any(), eq(DocumentType.INSURANCE), anyString(), eq(file)))
                    .thenReturn(savedDocument);
            when(documentRepository.save(savedDocument)).thenReturn(savedDocument);

            Document result = vehicleDocumentService.uploadVehicleDocument(command);

            assertThat(result).isNotNull();
            assertThat(result.getDocumentId()).isEqualTo(DOCUMENT_ID);
            verify(documentRepository).save(savedDocument);
        }

        @Test
        @DisplayName("Invalid file format - throws DOC-001")
        void upload_invalidFormat_throwsInvalidFileFormat() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.invalidFormatFile();

            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);
            command.setDocumentType("INSURANCE");

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> vehicleDocumentService.uploadVehicleDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.INVALID_FILE_FORMAT);
        }

        @Test
        @DisplayName("File too large - throws DOC-002")
        void upload_fileTooLarge_throwsFileTooLarge() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.oversizedFile();

            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);
            command.setDocumentType("INSURANCE");

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> vehicleDocumentService.uploadVehicleDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.FILE_TOO_LARGE);
        }

        @Test
        @DisplayName("Invalid document type (DRIVERS_LICENSE for vehicle) - throws VEH-006")
        void upload_invalidDocumentType_throwsInvalidVehicleDocumentType() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.validPdf();

            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);
            command.setDocumentType("DRIVERS_LICENSE");

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> vehicleDocumentService.uploadVehicleDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.INVALID_VEHICLE_DOCUMENT_TYPE);
        }

        @Test
        @DisplayName("Duplicate document - throws DOC-004")
        void upload_duplicateDocument_throwsDocumentAlreadyExists() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.validPdf();

            UploadVehicleDocumentCommand command = new UploadVehicleDocumentCommand();
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);
            command.setDocumentType("INSURANCE");

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(
                    eq(VEHICLE_ID), eq(DocumentType.INSURANCE), anyList())).thenReturn(true);

            assertThatThrownBy(() -> vehicleDocumentService.uploadVehicleDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ALREADY_EXISTS);
        }
    }

    // ── getVehicleDocuments ─────────────────────────────────────────

    @Nested
    @DisplayName("getVehicleDocuments()")
    class GetVehicleDocuments {

        @Test
        @DisplayName("Happy path - returns list of vehicle documents")
        void getDocuments_validOwner_returnsDocumentList() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(VEHICLE_ID);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findByVehicleId(VEHICLE_ID)).thenReturn(List.of(document));

            GetVehicleDocumentsCommand cmd = new GetVehicleDocumentsCommand();
            cmd.setVehicleId(VEHICLE_ID);
            cmd.setOwnerId(OWNER_ID);
            List<Document> result = vehicleDocumentService.getVehicleDocuments(cmd);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDocumentId()).isEqualTo(DOCUMENT_ID);
        }

        @Test
        @DisplayName("Wrong owner - throws VEH-005")
        void getDocuments_wrongOwner_throwsAccessDenied() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

            GetVehicleDocumentsCommand cmd = new GetVehicleDocumentsCommand();
            cmd.setVehicleId(VEHICLE_ID);
            cmd.setOwnerId(999L);
            assertThatThrownBy(() -> vehicleDocumentService.getVehicleDocuments(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_ACCESS_DENIED);
        }
    }

    // ── getDocumentForView / getDocumentForDownload ─────────────────

    @Nested
    @DisplayName("getDocumentForView()")
    class GetDocumentForView {

        @Test
        @DisplayName("Happy path - returns document")
        void getDocumentForView_validRequest_returnsDocument() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(VEHICLE_ID);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

            ViewVehicleDocumentCommand cmd = new ViewVehicleDocumentCommand();
            cmd.setDocumentId(DOCUMENT_ID);
            cmd.setVehicleId(VEHICLE_ID);
            cmd.setOwnerId(OWNER_ID);
            Document result = vehicleDocumentService.getDocumentForView(cmd);

            assertThat(result).isNotNull();
            assertThat(result.getDocumentId()).isEqualTo(DOCUMENT_ID);
        }

        @Test
        @DisplayName("Document not found - throws DOC-007")
        void getDocumentForView_notFound_throwsDocumentNotFound() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            ViewVehicleDocumentCommand cmd = new ViewVehicleDocumentCommand();
            cmd.setDocumentId(999L);
            cmd.setVehicleId(VEHICLE_ID);
            cmd.setOwnerId(OWNER_ID);
            assertThatThrownBy(() -> vehicleDocumentService.getDocumentForView(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Document doesn't belong to vehicle - throws DOC-006")
        void getDocumentForView_wrongVehicle_throwsAccessDenied() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(999L);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

            ViewVehicleDocumentCommand cmd = new ViewVehicleDocumentCommand();
            cmd.setDocumentId(DOCUMENT_ID);
            cmd.setVehicleId(VEHICLE_ID);
            cmd.setOwnerId(OWNER_ID);
            assertThatThrownBy(() -> vehicleDocumentService.getDocumentForView(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ACCESS_DENIED);
        }
    }

    // ── reuploadVehicleDocument ─────────────────────────────────────

    @Nested
    @DisplayName("reuploadVehicleDocument()")
    class ReuploadVehicleDocument {

        @Test
        @DisplayName("Happy path - replaces document successfully")
        void reupload_validRequest_returnsUpdatedDocument() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.validPdf();
            Document document = DocumentFixture.pendingLicense();
            document.setVehicleId(VEHICLE_ID);

            ReuploadVehicleDocumentCommand command = new ReuploadVehicleDocumentCommand();
            command.setDocumentId(DOCUMENT_ID);
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(fileStorageService.store(file, OWNER_ID, "DRIVERS_LICENSE")).thenReturn("uploads/20/license_new.pdf");
            when(documentRepository.save(document)).thenReturn(document);

            Document result = vehicleDocumentService.reuploadVehicleDocument(command);

            assertThat(result).isNotNull();
            assertThat(result.getDocumentId()).isEqualTo(DOCUMENT_ID);
            verify(fileStorageService).delete(document.getFileUrl());
            verify(documentServiceMapper).updateDocumentForReupload(anyString(), eq(file), eq(document));
        }

        @Test
        @DisplayName("Already approved - throws DOC-005")
        void reupload_alreadyApproved_throwsDocumentAlreadyApproved() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            MockMultipartFile file = DocumentFixture.validPdf();
            Document document = DocumentFixture.approvedLicense();
            document.setVehicleId(VEHICLE_ID);

            ReuploadVehicleDocumentCommand command = new ReuploadVehicleDocumentCommand();
            command.setDocumentId(DOCUMENT_ID);
            command.setVehicleId(VEHICLE_ID);
            command.setOwnerId(OWNER_ID);
            command.setFile(file);

            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

            assertThatThrownBy(() -> vehicleDocumentService.reuploadVehicleDocument(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.DOCUMENT_ALREADY_APPROVED);
        }
    }
}
