package com.turbo.vehicle.dto;

import com.turbo.vehicle.validation.VehicleValidationConstraints;
import com.turbo.vehicle.validation.VehicleValidationMessages;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVehicleRequest {

    @Pattern(regexp = VehicleValidationConstraints.TEXT_PATTERN, message = VehicleValidationMessages.MAKE_PATTERN)
    private String make;

    @Pattern(regexp = VehicleValidationConstraints.TEXT_PATTERN, message = VehicleValidationMessages.MODEL_PATTERN)
    private String model;

    private Integer year;

    @Pattern(regexp = VehicleValidationConstraints.LICENSE_PLATE_PATTERN, message = VehicleValidationMessages.LICENSE_PLATE_PATTERN_MSG)
    private String licensePlate;

    @Pattern(regexp = VehicleValidationConstraints.CATEGORY_PATTERN, message = VehicleValidationMessages.CATEGORY_PATTERN_MSG)
    private String category;

    @Pattern(regexp = VehicleValidationConstraints.FUEL_TYPE_PATTERN, message = VehicleValidationMessages.FUEL_TYPE_PATTERN_MSG)
    private String fuelType;

    @Pattern(regexp = VehicleValidationConstraints.DESCRIPTION_PATTERN, message = VehicleValidationMessages.DESCRIPTION_PATTERN_MSG)
    @Size(max = VehicleValidationConstraints.MAX_DESCRIPTION_LENGTH)
    private String description;
}
