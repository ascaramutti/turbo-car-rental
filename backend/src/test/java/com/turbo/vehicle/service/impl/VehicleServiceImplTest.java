package com.turbo.vehicle.service.impl;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.dto.VehicleClassificationCheckResponse;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.mapper.VehicleServiceMapper;
import com.turbo.vehicle.validation.VehicleValidationConstraints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleServiceImpl - checkClassificationReadiness()")
class VehicleServiceImplTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleServiceMapper vehicleServiceMapper;

    @InjectMocks private VehicleServiceImpl vehicleService;

    // ── Helpers ──────────────────────────────────────────────────────

    /** Creates a Document with the given type and status, linked to the fixture vehicle. */
    private Document buildDocument(DocumentType type, DocumentStatus status) {
        Document doc = new Document();
        doc.setDocumentId(1L);
        doc.setVehicleId(VehicleFixture.VEHICLE_ID);
        doc.setDocumentType(type);
        doc.setStatus(status);
        doc.setFileUrl("https://storage.test/file.pdf");
        doc.setFileName("file.pdf");
        doc.setFileSize(1024L);
        return doc;
    }

    /** Creates a vehicle with a specific year for taxi-age-eligibility tests. */
    private Vehicle vehicleWithYear(int year) {
        Vehicle vehicle = VehicleFixture.pendingVehicle();
        vehicle.setYear(year);
        return vehicle;
    }

    private void mockVehicleFound(Vehicle vehicle) {
        when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                .thenReturn(Optional.of(vehicle));
    }

    private void mockDocumentsForVehicle(List<Document> docs) {
        when(documentRepository.findByVehicleId(VehicleFixture.VEHICLE_ID))
                .thenReturn(docs);
    }

    // ── checkClassificationReadiness ─────────────────────────────────

    @Nested
    @DisplayName("checkClassificationReadiness()")
    class CheckClassificationReadiness {

        @Test
        @DisplayName("All 2 docs approved (no inspection) - ready=true, availableServiceTypes=[DELIVERY_ONLY]")
        void allDocsApproved_noInspection_readyWithDeliveryOnly() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            mockVehicleFound(vehicle);

            List<Document> docs = List.of(
                    buildDocument(DocumentType.INSURANCE, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.VEHICLE_REGISTRATION, DocumentStatus.APPROVED)
            );
            mockDocumentsForVehicle(docs);

            VehicleClassificationCheckResponse result =
                    vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID);

            assertThat(result.isReadyForClassification()).isTrue();
            assertThat(result.isHasApprovedInspection()).isFalse();
            assertThat(result.getAvailableServiceTypes()).containsExactly("DELIVERY_ONLY");
            assertThat(result.getVehicleId()).isEqualTo(VehicleFixture.VEHICLE_ID);
            assertThat(result.getVehicleMake()).isEqualTo(vehicle.getMake());
            assertThat(result.getVehicleModel()).isEqualTo(vehicle.getModel());
            assertThat(result.getVehicleYear()).isEqualTo(vehicle.getYear());
        }

        @Test
        @DisplayName("All 3 docs approved (with inspection), year <=9 years old - ready=true, availableServiceTypes=[TAXI_AND_DELIVERY, DELIVERY_ONLY]")
        void allDocsApproved_withInspection_youngVehicle_readyWithBothTypes() {
            int taxiEligibleYear = LocalDate.now().getYear() - VehicleValidationConstraints.MAX_TAXI_ELIGIBLE_AGE_YEARS;
            Vehicle vehicle = vehicleWithYear(taxiEligibleYear);
            mockVehicleFound(vehicle);

            List<Document> docs = List.of(
                    buildDocument(DocumentType.INSURANCE, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.VEHICLE_REGISTRATION, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.INSPECTION_REPORT, DocumentStatus.APPROVED)
            );
            mockDocumentsForVehicle(docs);

            VehicleClassificationCheckResponse result =
                    vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID);

            assertThat(result.isReadyForClassification()).isTrue();
            assertThat(result.isHasApprovedInspection()).isTrue();
            assertThat(result.getAvailableServiceTypes())
                    .containsExactly("TAXI_AND_DELIVERY", "DELIVERY_ONLY");
        }

        @Test
        @DisplayName("All 3 docs approved (with inspection), year >9 years old - ready=true, availableServiceTypes=[DELIVERY_ONLY]")
        void allDocsApproved_withInspection_oldVehicle_readyWithDeliveryOnly() {
            int tooOldYear = LocalDate.now().getYear() - VehicleValidationConstraints.MAX_TAXI_ELIGIBLE_AGE_YEARS - 1;
            Vehicle vehicle = vehicleWithYear(tooOldYear);
            mockVehicleFound(vehicle);

            List<Document> docs = List.of(
                    buildDocument(DocumentType.INSURANCE, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.VEHICLE_REGISTRATION, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.INSPECTION_REPORT, DocumentStatus.APPROVED)
            );
            mockDocumentsForVehicle(docs);

            VehicleClassificationCheckResponse result =
                    vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID);

            assertThat(result.isReadyForClassification()).isTrue();
            assertThat(result.isHasApprovedInspection()).isTrue();
            assertThat(result.getAvailableServiceTypes()).containsExactly("DELIVERY_ONLY");
        }

        @Test
        @DisplayName("Some docs pending - ready=false, empty availableServiceTypes")
        void someDocsPending_notReady() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            mockVehicleFound(vehicle);

            List<Document> docs = List.of(
                    buildDocument(DocumentType.INSURANCE, DocumentStatus.APPROVED),
                    buildDocument(DocumentType.VEHICLE_REGISTRATION, DocumentStatus.PENDING),
                    buildDocument(DocumentType.INSPECTION_REPORT, DocumentStatus.APPROVED)
            );
            mockDocumentsForVehicle(docs);

            VehicleClassificationCheckResponse result =
                    vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID);

            assertThat(result.isReadyForClassification()).isFalse();
            assertThat(result.getAvailableServiceTypes()).isEmpty();
        }

        @Test
        @DisplayName("No docs at all - ready=false, empty availableServiceTypes")
        void noDocs_notReady() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            mockVehicleFound(vehicle);
            mockDocumentsForVehicle(List.of());

            VehicleClassificationCheckResponse result =
                    vehicleService.checkClassificationReadiness(VehicleFixture.VEHICLE_ID);

            assertThat(result.isReadyForClassification()).isFalse();
            assertThat(result.getAvailableServiceTypes()).isEmpty();
            assertThat(result.isHasApprovedInspection()).isFalse();
        }

        @Test
        @DisplayName("Vehicle not found - throws VEH-004")
        void vehicleNotFound_throwsVehicleNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vehicleService.checkClassificationReadiness(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_FOUND);
        }
    }
}
