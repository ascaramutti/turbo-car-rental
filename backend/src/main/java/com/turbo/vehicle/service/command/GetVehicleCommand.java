package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetVehicleCommand {
    private Long vehicleId;
    private Long ownerId;
}
