package com.turbo.vehicle.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetVehicleDocumentsCommand {
    private Long vehicleId;
    private Long ownerId;
}
