package com.turbo.vehicle.dto;

import com.turbo.vehicle.validation.VehicleValidationConstraints;
import com.turbo.vehicle.validation.VehicleValidationMessages;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ActivateVehicleRequest {

    @NotNull(message = VehicleValidationMessages.AVAILABLE_UNTIL_REQUIRED)
    @Future(message = VehicleValidationMessages.AVAILABLE_UNTIL_FUTURE)
    private LocalDateTime availableUntil;

    @NotBlank(message = VehicleValidationMessages.LOCATION_REQUIRED)
    @Pattern(regexp = VehicleValidationConstraints.LOCATION_PATTERN, message = VehicleValidationMessages.LOCATION_PATTERN_MSG)
    @Size(max = VehicleValidationConstraints.MAX_LOCATION_LENGTH)
    private String generalLocation;

    @NotNull(message = VehicleValidationMessages.LATITUDE_REQUIRED)
    private Double latitude;

    @NotNull(message = VehicleValidationMessages.LONGITUDE_REQUIRED)
    private Double longitude;

    @NotNull(message = VehicleValidationMessages.HOURLY_RATE_REQUIRED)
    @DecimalMin(value = "1.00", message = VehicleValidationMessages.HOURLY_RATE_MIN)
    private BigDecimal hourlyRate;
}
