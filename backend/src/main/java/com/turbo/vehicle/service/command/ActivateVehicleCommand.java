package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ActivateVehicleCommand {
    private Long vehicleId;
    private Long ownerId;
    private LocalDateTime availableUntil;
    private String generalLocation;
    private Double latitude;
    private Double longitude;
    private BigDecimal hourlyRate;
}
