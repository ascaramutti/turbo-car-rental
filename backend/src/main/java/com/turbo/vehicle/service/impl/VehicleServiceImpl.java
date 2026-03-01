package com.turbo.vehicle.service.impl;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.repository.DocumentRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.User;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.dto.VehicleClassificationCheckResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.model.enums.VehicleStatus;
import com.turbo.vehicle.repository.VehicleRepository;
import com.turbo.vehicle.service.VehicleService;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.ApproveVehicleCommand;
import com.turbo.vehicle.service.command.DeactivateVehicleCommand;
import com.turbo.vehicle.service.command.GetVehicleCommand;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import com.turbo.vehicle.service.command.UpdateVehicleCommand;
import com.turbo.vehicle.service.mapper.VehicleServiceMapper;
import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.vehicle.validation.VehicleValidationConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final VehicleServiceMapper vehicleServiceMapper;

    /** Vehicle CRUD */

    @Override
    @Transactional
    public Vehicle registerVehicle(RegisterVehicleCommand command) {
        validateVehicleYear(command.getYear());
        validateVinUniqueness(command.getVin());
        validateLicensePlateUniqueness(command.getLicensePlate());

        CarOwner owner = findCarOwnerById(command.getOwnerId());
        Vehicle vehicle = vehicleServiceMapper.toEntity(command, owner);

        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getMyVehicles(Long ownerId) {
        return vehicleRepository.findByOwnerUserId(ownerId);
    }

    @Override
    public Vehicle getVehicleById(GetVehicleCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());
        return vehicle;
    }

    @Override
    @Transactional
    public Vehicle updateVehicle(UpdateVehicleCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());

        applyUpdates(vehicle, command);

        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public Vehicle approveVehicle(ApproveVehicleCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        ServiceType type = parseServiceType(command.getServiceType());
        validateTaxiEligibility(vehicle, type);
        validateInspectionForTaxi(vehicle, type);
        vehicle.setServiceType(type);
        vehicle.setStatus(VehicleStatus.APPROVED);
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public Vehicle activateVehicle(ActivateVehicleCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());
        validateVehicleIsApproved(vehicle);
        validateAvailableUntilIsFuture(command.getAvailableUntil());
        vehicle.setIsActive(true);
        vehicle.setAvailableUntil(command.getAvailableUntil());
        vehicle.setGeneralLocation(command.getGeneralLocation());
        vehicle.setLatitude(command.getLatitude());
        vehicle.setLongitude(command.getLongitude());
        vehicle.setHourlyRate(command.getHourlyRate());
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void deactivateVehicle(DeactivateVehicleCommand command) {
        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateOwnership(vehicle, command.getOwnerId());
        vehicle.setIsActive(false);
        vehicle.setAvailableUntil(null);
        vehicleRepository.save(vehicle);
    }

    /** Auto-deactivates vehicles whose availableUntil has expired (runs every 5 minutes). */
    @Scheduled(cron = BookingValidationConstraints.EVERY_FIVE_MINUTES_CRON)
    @Transactional
    public void autoDeactivateExpiredVehicles() {
        List<Vehicle> expired = vehicleRepository.findExpiredActiveVehicles(LocalDateTime.now());
        expired.forEach(v -> {
            v.setIsActive(false);
            v.setAvailableUntil(null);
        });
        if (!expired.isEmpty()) {
            vehicleRepository.saveAll(expired);
            log.info("Auto-deactivated {} expired vehicles", expired.size());
        }
    }

    @Override
    public VehicleClassificationCheckResponse checkClassificationReadiness(Long vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        List<Document> vehicleDocs = documentRepository.findByVehicleId(vehicleId);

        boolean allApproved = !vehicleDocs.isEmpty()
                && vehicleDocs.stream().allMatch(d -> d.getStatus() == DocumentStatus.APPROVED);

        boolean hasApprovedInspection = vehicleDocs.stream()
                .anyMatch(d -> d.getDocumentType() == DocumentType.INSPECTION_REPORT
                        && d.getStatus() == DocumentStatus.APPROVED);

        List<String> availableServiceTypes = allApproved
                ? determineAvailableServiceTypes(vehicle.getYear(), hasApprovedInspection)
                : List.of();

        List<VehicleClassificationCheckResponse.VehicleDocumentInfo> docInfos = vehicleDocs.stream()
                .map(d -> new VehicleClassificationCheckResponse.VehicleDocumentInfo(
                        d.getDocumentId(),
                        d.getDocumentType().name(),
                        d.getStatus().name(),
                        d.getFileName()))
                .toList();

        return new VehicleClassificationCheckResponse(
                allApproved,
                vehicle.getVehicleId(),
                vehicle.getYear(),
                vehicle.getMake(),
                vehicle.getModel(),
                hasApprovedInspection,
                availableServiceTypes,
                docInfos
        );
    }

    /** Determines available service types based on vehicle year and inspection status. */
    private List<String> determineAvailableServiceTypes(Integer vehicleYear, boolean hasApprovedInspection) {
        int currentYear = LocalDate.now().getYear();
        boolean isTaxiEligibleByAge = (currentYear - vehicleYear) <= VehicleValidationConstraints.MAX_TAXI_ELIGIBLE_AGE_YEARS;

        List<String> types = new ArrayList<>();
        if (isTaxiEligibleByAge && hasApprovedInspection) {
            types.add(ServiceType.TAXI_AND_DELIVERY.name());
        }
        types.add(ServiceType.DELIVERY_ONLY.name());
        return types;
    }

    /** Lookup helpers */

    /** Finds a user by ID and verifies they are a CarOwner, or throws. */
    private CarOwner findCarOwnerById(Long ownerId) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
        if (!(user instanceof CarOwner carOwner)) {
            throw new BusinessException(VehicleErrorCode.NOT_A_CAR_OWNER);
        }
        return carOwner;
    }

    /** Parses the service type string or throws if invalid. */
    private ServiceType parseServiceType(String serviceType) {
        try {
            return ServiceType.valueOf(serviceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(VehicleErrorCode.INVALID_SERVICE_TYPE);
        }
    }

    /** Parses the vehicle category string or throws if invalid. */
    private VehicleCategory parseCategory(String category) {
        try {
            return VehicleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(VehicleErrorCode.INVALID_CATEGORY);
        }
    }

    /** Parses the fuel type string or throws if invalid. */
    private FuelType parseFuelType(String fuelType) {
        try {
            return FuelType.valueOf(fuelType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(VehicleErrorCode.INVALID_FUEL_TYPE);
        }
    }

    /** Finds a vehicle by ID or throws if not found. */
    private Vehicle findVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));
    }

    /** Vehicle validation helpers */

    /** Validates that the vehicle year is within the allowed range. */
    private void validateVehicleYear(Integer year) {
        int currentYear = LocalDate.now().getYear();
        int oldestAllowedYear = currentYear - VehicleValidationConstraints.MAX_VEHICLE_AGE_YEARS;
        int newestAllowedYear = currentYear + VehicleValidationConstraints.MAX_VEHICLE_FUTURE_YEARS;
        if (year < oldestAllowedYear) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_YEAR_TOO_OLD);
        }
        if (year > newestAllowedYear) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_YEAR_TOO_NEW);
        }
    }

    /** Validates that vehicles older than 9 years cannot be assigned TAXI_AND_DELIVERY. */
    private void validateTaxiEligibility(Vehicle vehicle, ServiceType serviceType) {
        if (serviceType == ServiceType.TAXI_AND_DELIVERY) {
            int oldestTaxiYear = LocalDate.now().getYear() - VehicleValidationConstraints.MAX_TAXI_ELIGIBLE_AGE_YEARS;
            if (vehicle.getYear() < oldestTaxiYear) {
                throw new BusinessException(VehicleErrorCode.TAXI_AGE_EXCEEDED);
            }
        }
    }

    /** Validates that INSPECTION_REPORT is approved before assigning TAXI_AND_DELIVERY. */
    private void validateInspectionForTaxi(Vehicle vehicle, ServiceType serviceType) {
        if (serviceType == ServiceType.TAXI_AND_DELIVERY) {
            boolean hasApprovedInspection = documentRepository.existsByVehicleIdAndDocumentTypeAndStatusIn(
                    vehicle.getVehicleId(), DocumentType.INSPECTION_REPORT, List.of(DocumentStatus.APPROVED));
            if (!hasApprovedInspection) {
                throw new BusinessException(VehicleErrorCode.INSPECTION_REQUIRED_FOR_TAXI);
            }
        }
    }

    /** Validates that no other vehicle is registered with the same VIN. */
    private void validateVinUniqueness(String vin) {
        if (vehicleRepository.existsByVin(vin)) {
            throw new BusinessException(VehicleErrorCode.DUPLICATE_VIN);
        }
    }

    /** Validates that no other vehicle is registered with the same license plate. */
    private void validateLicensePlateUniqueness(String licensePlate) {
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new BusinessException(VehicleErrorCode.DUPLICATE_LICENSE_PLATE);
        }
    }

    /** Validates that the vehicle belongs to the requesting user. */
    private void validateOwnership(Vehicle vehicle, Long ownerId) {
        if (!vehicle.getOwner().getUserId().equals(ownerId)) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_ACCESS_DENIED);
        }
    }

    /** Validates that the vehicle is approved by admin before owner can activate. */
    private void validateVehicleIsApproved(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.APPROVED) {
            throw new BusinessException(VehicleErrorCode.VEHICLE_NOT_APPROVED);
        }
    }

    /** Validates that availableUntil is in the future. */
    private void validateAvailableUntilIsFuture(LocalDateTime availableUntil) {
        if (availableUntil == null || !availableUntil.isAfter(LocalDateTime.now())) {
            throw new BusinessException(VehicleErrorCode.AVAILABLE_UNTIL_MUST_BE_FUTURE);
        }
    }

    /** Update helpers */

    /** Applies non-null fields from the update command to the vehicle entity. */
    private void applyUpdates(Vehicle vehicle, UpdateVehicleCommand command) {
        applyBasicInfoUpdates(command, vehicle);
        applyCategoryUpdates(command, vehicle);
    }

    /** Applies make, model, year, and description updates if present. */
    private void applyBasicInfoUpdates(UpdateVehicleCommand command, Vehicle vehicle) {
        if (command.getMake() != null) {
            vehicle.setMake(command.getMake());
        }
        if (command.getModel() != null) {
            vehicle.setModel(command.getModel());
        }
        if (command.getYear() != null) {
            validateVehicleYear(command.getYear());
            vehicle.setYear(command.getYear());
        }
        if (command.getDescription() != null) {
            vehicle.setDescription(command.getDescription());
        }
    }

    /** Applies category, fuel type, and license plate updates with validation. */
    private void applyCategoryUpdates(UpdateVehicleCommand command, Vehicle vehicle) {
        if (command.getCategory() != null) {
            vehicle.setCategory(parseCategory(command.getCategory()));
        }
        if (command.getFuelType() != null) {
            vehicle.setFuelType(parseFuelType(command.getFuelType()));
        }
        if (command.getLicensePlate() != null && !command.getLicensePlate().equals(vehicle.getLicensePlate())) {
            validateLicensePlateUniqueness(command.getLicensePlate());
            vehicle.setLicensePlate(command.getLicensePlate());
        }
    }
}
