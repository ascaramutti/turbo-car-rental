package com.turbo.vehicle.service;

import com.turbo.vehicle.dto.VehicleClassificationCheckResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.ApproveVehicleCommand;
import com.turbo.vehicle.service.command.DeactivateVehicleCommand;
import com.turbo.vehicle.service.command.GetVehicleCommand;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import com.turbo.vehicle.service.command.UpdateVehicleCommand;

import java.util.List;

public interface VehicleService {

    Vehicle registerVehicle(RegisterVehicleCommand command);

    List<Vehicle> getMyVehicles(Long ownerId);

    Vehicle getVehicleById(GetVehicleCommand command);

    Vehicle updateVehicle(UpdateVehicleCommand command);

    Vehicle approveVehicle(ApproveVehicleCommand command);

    Vehicle activateVehicle(ActivateVehicleCommand command);

    void deactivateVehicle(DeactivateVehicleCommand command);

    VehicleClassificationCheckResponse checkClassificationReadiness(Long vehicleId);
}
