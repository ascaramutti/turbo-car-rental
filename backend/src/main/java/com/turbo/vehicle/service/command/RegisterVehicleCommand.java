package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVehicleCommand {

    private Long ownerId;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String licensePlate;
    private String category;
    private String fuelType;
    private String description;
}
