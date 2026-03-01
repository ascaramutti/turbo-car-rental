package com.turbo.vehicle.service.impl;

import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.model.enums.VehicleStatus;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.ApproveVehicleCommand;
import com.turbo.vehicle.service.command.DeactivateVehicleCommand;
import com.turbo.vehicle.service.command.GetVehicleCommand;
import com.turbo.vehicle.service.command.UpdateVehicleCommand;
import com.turbo.vehicle.service.mapper.VehicleServiceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleServiceImpl - CRUD operations")
class VehicleServiceCrudTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleServiceMapper vehicleServiceMapper;

    @InjectMocks private VehicleServiceImpl vehicleService;

    /** getMyVehicles */

    @Nested
    @DisplayName("getMyVehicles()")
    class GetMyVehicles {

        @Test
        @DisplayName("Returns list of vehicles for owner")
        void getMyVehicles_returnsVehicleList() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findByOwnerUserId(VehicleFixture.OWNER_USER_ID))
                    .thenReturn(List.of(vehicle));

            List<Vehicle> result = vehicleService.getMyVehicles(VehicleFixture.OWNER_USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVehicleId()).isEqualTo(VehicleFixture.VEHICLE_ID);
        }

        @Test
        @DisplayName("Returns empty list when no vehicles found")
        void getMyVehicles_noVehicles_returnsEmptyList() {
            when(vehicleRepository.findByOwnerUserId(VehicleFixture.OWNER_USER_ID))
                    .thenReturn(List.of());

            List<Vehicle> result = vehicleService.getMyVehicles(VehicleFixture.OWNER_USER_ID);

            assertThat(result).isEmpty();
        }
    }

    /** getVehicleById */

    @Nested
    @DisplayName("getVehicleById()")
    class GetVehicleById {

        @Test
        @DisplayName("Happy path - returns vehicle for valid owner")
        void getVehicleById_validOwner_returnsVehicle() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            GetVehicleCommand command = new GetVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);

            Vehicle result = vehicleService.getVehicleById(command);

            assertThat(result).isNotNull();
            assertThat(result.getVehicleId()).isEqualTo(VehicleFixture.VEHICLE_ID);
        }

        @Test
        @DisplayName("Vehicle not found - throws VEH-004")
        void getVehicleById_notFound_throwsVehicleNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            GetVehicleCommand command = new GetVehicleCommand();
            command.setVehicleId(999L);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);

            assertThatThrownBy(() -> vehicleService.getVehicleById(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_FOUND);
        }

        @Test
        @DisplayName("Wrong owner - throws VEH-005")
        void getVehicleById_wrongOwner_throwsAccessDenied() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            GetVehicleCommand command = new GetVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(999L);

            assertThatThrownBy(() -> vehicleService.getVehicleById(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_ACCESS_DENIED);
        }
    }

    /** updateVehicle */

    @Nested
    @DisplayName("updateVehicle()")
    class UpdateVehicle {

        @Test
        @DisplayName("Happy path - updates vehicle successfully")
        void updateVehicle_validRequest_returnsUpdatedVehicle() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            UpdateVehicleCommand command = new UpdateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setMake("Honda");

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Vehicle result = vehicleService.updateVehicle(command);

            assertThat(result).isNotNull();
            assertThat(result.getMake()).isEqualTo("Honda");
            verify(vehicleRepository).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("Duplicate license plate on update - throws VEH-003")
        void updateVehicle_duplicatePlate_throwsDuplicateLicensePlate() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            UpdateVehicleCommand command = new UpdateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setLicensePlate("XYZ 9999");

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.existsByLicensePlate("XYZ 9999")).thenReturn(true);

            assertThatThrownBy(() -> vehicleService.updateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.DUPLICATE_LICENSE_PLATE);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Invalid category - throws VEH-010")
        void updateVehicle_invalidCategory_throwsInvalidCategory() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            UpdateVehicleCommand command = new UpdateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setCategory("INVALID_CATEGORY");

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> vehicleService.updateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.INVALID_CATEGORY);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Invalid fuel type - throws VEH-011")
        void updateVehicle_invalidFuelType_throwsInvalidFuelType() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            UpdateVehicleCommand command = new UpdateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setFuelType("NUCLEAR");

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            assertThatThrownBy(() -> vehicleService.updateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.INVALID_FUEL_TYPE);

            verify(vehicleRepository, never()).save(any());
        }
    }

    /** deactivateVehicle */

    @Nested
    @DisplayName("deactivateVehicle()")
    class DeactivateVehicle {

        @Test
        @DisplayName("Happy path - sets vehicle to inactive and clears availableUntil")
        void deactivateVehicle_validRequest_setsInactiveAndClearsAvailableUntil() {
            Vehicle vehicle = VehicleFixture.activeVehicle();
            vehicle.setAvailableUntil(LocalDateTime.now().plusDays(7));
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DeactivateVehicleCommand command = new DeactivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);

            vehicleService.deactivateVehicle(command);

            assertThat(vehicle.getIsActive()).isFalse();
            assertThat(vehicle.getAvailableUntil()).isNull();
            verify(vehicleRepository).save(vehicle);
        }

        @Test
        @DisplayName("Vehicle not found - throws VEH-004")
        void deactivateVehicle_notFound_throwsVehicleNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            DeactivateVehicleCommand command = new DeactivateVehicleCommand();
            command.setVehicleId(999L);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);

            assertThatThrownBy(() -> vehicleService.deactivateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_FOUND);
        }

        @Test
        @DisplayName("Wrong owner - throws VEH-005")
        void deactivateVehicle_wrongOwner_throwsAccessDenied() {
            Vehicle vehicle = VehicleFixture.activeVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            DeactivateVehicleCommand command = new DeactivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(999L);

            assertThatThrownBy(() -> vehicleService.deactivateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_ACCESS_DENIED);

            verify(vehicleRepository, never()).save(any());
        }
    }

    /** approveVehicle (admin) */

    @Nested
    @DisplayName("approveVehicle()")
    class ApproveVehicle {

        @Test
        @DisplayName("Happy path - approves with TAXI_AND_DELIVERY when inspection is approved")
        void approveVehicle_taxiAndDelivery_setsApprovedAndServiceType() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(
                    VehicleFixture.VEHICLE_ID, DocumentType.INSPECTION_REPORT, List.of(DocumentStatus.APPROVED)))
                    .thenReturn(true);
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("TAXI_AND_DELIVERY");

            Vehicle result = vehicleService.approveVehicle(command);

            assertThat(result.getServiceType()).isEqualTo(ServiceType.TAXI_AND_DELIVERY);
            assertThat(result.getStatus()).isEqualTo(VehicleStatus.APPROVED);
            assertThat(result.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("VEH-015: TAXI_AND_DELIVERY without approved inspection report")
        void approveVehicle_taxiWithoutInspection_throwsInspectionRequired() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(
                    VehicleFixture.VEHICLE_ID, DocumentType.INSPECTION_REPORT, List.of(DocumentStatus.APPROVED)))
                    .thenReturn(false);

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("TAXI_AND_DELIVERY");

            assertThatThrownBy(() -> vehicleService.approveVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.INSPECTION_REQUIRED_FOR_TAXI);
        }

        @Test
        @DisplayName("Happy path - approves with DELIVERY_ONLY")
        void approveVehicle_deliveryOnly_setsApprovedAndServiceType() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("DELIVERY_ONLY");

            Vehicle result = vehicleService.approveVehicle(command);

            assertThat(result.getServiceType()).isEqualTo(ServiceType.DELIVERY_ONLY);
        }

        @Test
        @DisplayName("Invalid service type - throws VEH-008")
        void approveVehicle_invalidServiceType_throwsError() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("INVALID");

            assertThatThrownBy(() -> vehicleService.approveVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.INVALID_SERVICE_TYPE);
        }

        @Test
        @DisplayName("Vehicle not found - throws VEH-004")
        void approveVehicle_notFound_throwsVehicleNotFound() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(999L);
            command.setServiceType("TAXI_AND_DELIVERY");

            assertThatThrownBy(() -> vehicleService.approveVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_FOUND);
        }

        @Test
        @DisplayName("Old vehicle with TAXI_AND_DELIVERY - throws VEH-013")
        void approveVehicle_oldVehicleTaxi_throwsTaxiAgeExceeded() {
            Vehicle vehicle = VehicleFixture.oldPendingVehicle();

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("TAXI_AND_DELIVERY");

            assertThatThrownBy(() -> vehicleService.approveVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.TAXI_AGE_EXCEEDED);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Old vehicle with DELIVERY_ONLY - succeeds")
        void approveVehicle_oldVehicleDeliveryOnly_succeeds() {
            Vehicle vehicle = VehicleFixture.oldPendingVehicle();

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApproveVehicleCommand command = new ApproveVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setServiceType("DELIVERY_ONLY");

            Vehicle result = vehicleService.approveVehicle(command);

            assertThat(result.getServiceType()).isEqualTo(ServiceType.DELIVERY_ONLY);
            assertThat(result.getStatus()).isEqualTo(VehicleStatus.APPROVED);
        }
    }

    /** activateVehicle (owner) */

    @Nested
    @DisplayName("activateVehicle()")
    class ActivateVehicle {

        @Test
        @DisplayName("Happy path - owner activates approved vehicle with future availableUntil, location, and hourlyRate")
        void activateVehicle_approved_setsIsActiveTrueAndAvailableUntil() {
            Vehicle vehicle = VehicleFixture.approvedVehicle();
            LocalDateTime futureDate = LocalDateTime.now().plusDays(7);
            BigDecimal hourlyRate = new BigDecimal("15.00");

            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));
            when(vehicleRepository.save(any(Vehicle.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setAvailableUntil(futureDate);
            command.setGeneralLocation("Downtown Montreal");
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setHourlyRate(hourlyRate);

            Vehicle result = vehicleService.activateVehicle(command);

            assertThat(result.getIsActive()).isTrue();
            assertThat(result.getAvailableUntil()).isEqualTo(futureDate);
            assertThat(result.getStatus()).isEqualTo(VehicleStatus.APPROVED);
            assertThat(result.getGeneralLocation()).isEqualTo("Downtown Montreal");
            assertThat(result.getLatitude()).isEqualTo(45.5017);
            assertThat(result.getLongitude()).isEqualTo(-73.5673);
            assertThat(result.getHourlyRate()).isEqualTo(hourlyRate);
        }

        @Test
        @DisplayName("Vehicle not approved - throws VEH-009")
        void activateVehicle_notApproved_throwsError() {
            Vehicle vehicle = VehicleFixture.pendingVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setAvailableUntil(LocalDateTime.now().plusDays(7));
            command.setGeneralLocation("Downtown Montreal");
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setHourlyRate(new BigDecimal("15.00"));

            assertThatThrownBy(() -> vehicleService.activateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_APPROVED);
        }

        @Test
        @DisplayName("Wrong owner - throws VEH-005")
        void activateVehicle_wrongOwner_throwsAccessDenied() {
            Vehicle vehicle = VehicleFixture.approvedVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(999L);
            command.setAvailableUntil(LocalDateTime.now().plusDays(7));
            command.setGeneralLocation("Downtown Montreal");
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setHourlyRate(new BigDecimal("15.00"));

            assertThatThrownBy(() -> vehicleService.activateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_ACCESS_DENIED);
        }

        @Test
        @DisplayName("Available until in the past - throws VEH-012")
        void activateVehicle_pastAvailableUntil_throwsError() {
            Vehicle vehicle = VehicleFixture.approvedVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setAvailableUntil(LocalDateTime.now().minusDays(1));
            command.setGeneralLocation("Downtown Montreal");
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setHourlyRate(new BigDecimal("15.00"));

            assertThatThrownBy(() -> vehicleService.activateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.AVAILABLE_UNTIL_MUST_BE_FUTURE);
        }

        @Test
        @DisplayName("Null available until - throws VEH-012")
        void activateVehicle_nullAvailableUntil_throwsError() {
            Vehicle vehicle = VehicleFixture.approvedVehicle();
            when(vehicleRepository.findById(VehicleFixture.VEHICLE_ID))
                    .thenReturn(Optional.of(vehicle));

            ActivateVehicleCommand command = new ActivateVehicleCommand();
            command.setVehicleId(VehicleFixture.VEHICLE_ID);
            command.setOwnerId(VehicleFixture.OWNER_USER_ID);
            command.setAvailableUntil(null);
            command.setGeneralLocation("Downtown Montreal");
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setHourlyRate(new BigDecimal("15.00"));

            assertThatThrownBy(() -> vehicleService.activateVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.AVAILABLE_UNTIL_MUST_BE_FUTURE);
        }
    }
}
