package com.turbo.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private Long vehicleId;
    private Long ownerId;
    private String ownerFullName;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String licensePlate;
    private String category;
    private String fuelType;
    private BigDecimal hourlyRate;
    private String description;
    private String generalLocation;
    private Double latitude;
    private Double longitude;
    private String status;
    private String serviceType;
    private Boolean isActive;
    private String availableUntil;
    private String createdAt;
}
