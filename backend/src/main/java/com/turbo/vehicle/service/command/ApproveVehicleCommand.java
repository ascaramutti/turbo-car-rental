package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveVehicleCommand {
    private Long vehicleId;
    private String serviceType;
}
