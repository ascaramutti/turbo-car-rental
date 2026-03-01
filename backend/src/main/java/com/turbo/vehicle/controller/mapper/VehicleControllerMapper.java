package com.turbo.vehicle.controller.mapper;

import com.turbo.vehicle.dto.ActivateVehicleRequest;
import com.turbo.vehicle.dto.RegisterVehicleRequest;
import com.turbo.vehicle.dto.UpdateVehicleRequest;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.DeactivateVehicleCommand;
import com.turbo.vehicle.service.command.GetVehicleCommand;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import com.turbo.vehicle.service.command.UpdateVehicleCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleControllerMapper {

    // ── Request → Command mappings ──────────────────────────────────

    @Mapping(source = "ownerId", target = "ownerId")
    RegisterVehicleCommand toRegisterCommand(RegisterVehicleRequest request, Long ownerId);

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    UpdateVehicleCommand toUpdateCommand(UpdateVehicleRequest request, Long vehicleId, Long ownerId);

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    GetVehicleCommand toGetVehicleCommand(Long vehicleId, Long ownerId);

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    @Mapping(source = "request.availableUntil", target = "availableUntil")
    @Mapping(source = "request.generalLocation", target = "generalLocation")
    @Mapping(source = "request.latitude", target = "latitude")
    @Mapping(source = "request.longitude", target = "longitude")
    @Mapping(source = "request.hourlyRate", target = "hourlyRate")
    ActivateVehicleCommand toActivateVehicleCommand(Long vehicleId, Long ownerId, ActivateVehicleRequest request);

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    DeactivateVehicleCommand toDeactivateVehicleCommand(Long vehicleId, Long ownerId);

    // ── Entity → Response mappings ──────────────────────────────────

    @Mapping(source = "owner.userId", target = "ownerId")
    @Mapping(target = "ownerFullName", expression = "java(vehicle.getOwner().getFirstName() + \" \" + vehicle.getOwner().getLastName())")
    @Mapping(target = "category", expression = "java(vehicle.getCategory().name())")
    @Mapping(target = "fuelType", expression = "java(vehicle.getFuelType().name())")
    @Mapping(target = "status", expression = "java(vehicle.getStatus().name())")
    @Mapping(target = "serviceType", expression = "java(vehicle.getServiceType() != null ? vehicle.getServiceType().name() : null)")
    @Mapping(target = "availableUntil", expression = "java(vehicle.getAvailableUntil() != null ? vehicle.getAvailableUntil().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(vehicle.getCreatedAt() != null ? vehicle.getCreatedAt().toString() : null)")
    VehicleResponse toVehicleResponse(Vehicle vehicle);

    List<VehicleResponse> toVehicleResponseList(List<Vehicle> vehicles);
}
