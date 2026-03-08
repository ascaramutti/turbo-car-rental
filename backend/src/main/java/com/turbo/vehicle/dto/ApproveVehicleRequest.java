package com.turbo.vehicle.dto;

import com.turbo.vehicle.validation.VehicleValidationConstraints;
import com.turbo.vehicle.validation.VehicleValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveVehicleRequest {

    @NotBlank(message = VehicleValidationMessages.SERVICE_TYPE_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.SERVICE_TYPE_PATTERN, message = VehicleValidationMessages.SERVICE_TYPE_PATTERN_MSG)
    private String serviceType;
}
