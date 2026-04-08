package com.turbo.vehicle.dto;

import com.turbo.vehicle.validation.VehicleValidationConstraints;
import com.turbo.vehicle.validation.VehicleValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVehicleRequest {

    @NotBlank(message = VehicleValidationMessages.VIN_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.VIN_PATTERN, message = VehicleValidationMessages.VIN_PATTERN)
    private String vin;

    @NotBlank(message = VehicleValidationMessages.MAKE_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.TEXT_PATTERN, message = VehicleValidationMessages.MAKE_PATTERN)
    private String make;

    @NotBlank(message = VehicleValidationMessages.MODEL_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.TEXT_PATTERN, message = VehicleValidationMessages.MODEL_PATTERN)
    private String model;

    @NotNull(message = VehicleValidationMessages.YEAR_REQUIRED)
    private Integer year;

    @NotBlank(message = VehicleValidationMessages.LICENSE_PLATE_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.LICENSE_PLATE_PATTERN, message = VehicleValidationMessages.LICENSE_PLATE_PATTERN_MSG)
    private String licensePlate;

    @NotBlank(message = VehicleValidationMessages.CATEGORY_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.CATEGORY_PATTERN, message = VehicleValidationMessages.CATEGORY_PATTERN_MSG)
    private String category;

    @NotBlank(message = VehicleValidationMessages.FUEL_TYPE_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.FUEL_TYPE_PATTERN, message = VehicleValidationMessages.FUEL_TYPE_PATTERN_MSG)
    private String fuelType;

    @Pattern(regexp = VehicleValidationConstraints.DESCRIPTION_PATTERN, message = VehicleValidationMessages.DESCRIPTION_PATTERN_MSG)
    @Size(max = VehicleValidationConstraints.MAX_DESCRIPTION_LENGTH)
    private String description;
}
