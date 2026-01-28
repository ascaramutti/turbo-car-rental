package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VehicleErrorCode implements ErrorCode {

    VEHICLE_YEAR_TOO_OLD("VEH-001", "Vehicle year exceeds the maximum allowed age of 20 years", HttpStatus.BAD_REQUEST),
    DUPLICATE_VIN("VEH-002", "A vehicle with this VIN is already registered", HttpStatus.CONFLICT),
    DUPLICATE_LICENSE_PLATE("VEH-003", "A vehicle with this license plate is already registered", HttpStatus.CONFLICT),
    VEHICLE_NOT_FOUND("VEH-004", "Vehicle not found", HttpStatus.NOT_FOUND),
    VEHICLE_ACCESS_DENIED("VEH-005", "You do not have permission to access this vehicle", HttpStatus.FORBIDDEN),
    INVALID_VEHICLE_DOCUMENT_TYPE("VEH-006", "Invalid document type. Only INSURANCE, VEHICLE_REGISTRATION, and INSPECTION_REPORT are allowed", HttpStatus.BAD_REQUEST),
    NOT_A_CAR_OWNER("VEH-007", "Only car owners can manage vehicles", HttpStatus.FORBIDDEN),
    INVALID_SERVICE_TYPE("VEH-008", "Invalid service type. Must be TAXI_AND_DELIVERY or DELIVERY_ONLY", HttpStatus.BAD_REQUEST),
    VEHICLE_NOT_APPROVED("VEH-009", "Vehicle must be approved by admin before it can be listed for rent", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY("VEH-010", "Invalid vehicle category", HttpStatus.BAD_REQUEST),
    INVALID_FUEL_TYPE("VEH-011", "Invalid fuel type", HttpStatus.BAD_REQUEST),
    AVAILABLE_UNTIL_MUST_BE_FUTURE("VEH-012", "Available until must be a future date/time", HttpStatus.BAD_REQUEST),
    TAXI_AGE_EXCEEDED("VEH-013", "Vehicles older than 9 years can only be registered as DELIVERY_ONLY", HttpStatus.BAD_REQUEST),
    VEHICLE_YEAR_TOO_NEW("VEH-014", "Vehicle year cannot be more than 1 year in the future", HttpStatus.BAD_REQUEST),
    INSPECTION_REQUIRED_FOR_TAXI("VEH-015", "Approved inspection report (CVIP) is required for TAXI_AND_DELIVERY service type", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
