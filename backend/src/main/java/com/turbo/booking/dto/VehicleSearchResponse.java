package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSearchResponse {

    private Long vehicleId;
    private String make;
    private String model;
    private Integer year;
    private String category;
    private String fuelType;
    private String serviceType;
    private BigDecimal hourlyRate;
    private String description;
    private String generalLocation;
    private Double maskedLatitude;
    private Double maskedLongitude;
    private String availableUntil;
    private String ownerFullName;
    private Float ownerRating;
    private String effectiveServiceType;
    private String serviceTypeWarning;
}
