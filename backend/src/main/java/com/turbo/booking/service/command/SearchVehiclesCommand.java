package com.turbo.booking.service.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SearchVehiclesCommand {

    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String category;
    private String serviceType;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String fuelType;
}
