package com.turbo.vehicle.service.impl;

import com.turbo.document.repository.DocumentRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.model.Driver;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.fixture.VehicleFixture;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.VehicleStatus;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import com.turbo.vehicle.service.mapper.VehicleServiceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleServiceImpl - registerVehicle()")
class VehicleServiceRegisterTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehicleServiceMapper vehicleServiceMapper;

    @InjectMocks private VehicleServiceImpl vehicleService;

    // ── Helpers ──────────────────────────────────────────────────────

    private void mockOwnerExists() {
        when(userRepository.findById(VehicleFixture.OWNER_USER_ID))
                .thenReturn(Optional.of(VehicleFixture.testCarOwner()));
    }

    private void mockNoVinDuplicate() {
        when(vehicleRepository.existsByVin(any())).thenReturn(false);
    }

    private void mockNoLicensePlateDuplicate() {
        when(vehicleRepository.existsByLicensePlate(any())).thenReturn(false);
    }

    private void mockMapperAndSave() {
        Vehicle vehicle = VehicleFixture.pendingVehicle();
        when(vehicleServiceMapper.toEntity(any(), any())).thenReturn(vehicle);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Happy paths ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Valid registration - returns Vehicle with PENDING status")
        void registerVehicle_validRequest_returnsPendingVehicle() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            mockNoVinDuplicate();
            mockNoLicensePlateDuplicate();
            mockOwnerExists();
            mockMapperAndSave();

            Vehicle result = vehicleService.registerVehicle(command);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(VehicleStatus.PENDING);
            assertThat(result.getVehicleId()).isEqualTo(VehicleFixture.VEHICLE_ID);
            verify(vehicleRepository).save(any(Vehicle.class));
        }
    }

    // ── Unhappy paths ────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Year too old - throws VEH-001")
        void registerVehicle_yearTooOld_throwsVehicleYearTooOld() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            command.setYear(2000);

            assertThatThrownBy(() -> vehicleService.registerVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_YEAR_TOO_OLD);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Year too new (future) - throws VEH-014")
        void registerVehicle_yearTooNew_throwsVehicleYearTooNew() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            command.setYear(2030);

            assertThatThrownBy(() -> vehicleService.registerVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_YEAR_TOO_NEW);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate VIN - throws VEH-002")
        void registerVehicle_duplicateVin_throwsDuplicateVin() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            when(vehicleRepository.existsByVin(command.getVin())).thenReturn(true);

            assertThatThrownBy(() -> vehicleService.registerVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.DUPLICATE_VIN);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate license plate - throws VEH-003")
        void registerVehicle_duplicatePlate_throwsDuplicateLicensePlate() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            mockNoVinDuplicate();
            when(vehicleRepository.existsByLicensePlate(command.getLicensePlate())).thenReturn(true);

            assertThatThrownBy(() -> vehicleService.registerVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.DUPLICATE_LICENSE_PLATE);

            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-CarOwner (Driver user) - throws VEH-007")
        void registerVehicle_notACarOwner_throwsNotACarOwner() {
            RegisterVehicleCommand command = VehicleFixture.registerVehicleCommand();
            Driver driver = VehicleFixture.testDriver();
            command.setOwnerId(driver.getUserId());
            mockNoVinDuplicate();
            mockNoLicensePlateDuplicate();
            when(userRepository.findById(driver.getUserId()))
                    .thenReturn(Optional.of(driver));

            assertThatThrownBy(() -> vehicleService.registerVehicle(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.NOT_A_CAR_OWNER);

            verify(vehicleRepository, never()).save(any());
        }
    }
}
