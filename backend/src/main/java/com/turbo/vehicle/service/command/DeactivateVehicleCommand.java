package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeactivateVehicleCommand {
    private Long vehicleId;
    private Long ownerId;
}
