package com.turbo.vehicle.controller.mapper;

import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.service.command.ApproveVehicleCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminVehicleControllerMapper {

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "serviceType", target = "serviceType")
    ApproveVehicleCommand toApproveCommand(Long vehicleId, String serviceType);

    @Mapping(source = "owner.userId", target = "ownerId")
    @Mapping(target = "ownerFullName", expression = "java(vehicle.getOwner().getFirstName() + \" \" + vehicle.getOwner().getLastName())")
    @Mapping(target = "category", expression = "java(vehicle.getCategory().name())")
    @Mapping(target = "fuelType", expression = "java(vehicle.getFuelType().name())")
    @Mapping(target = "status", expression = "java(vehicle.getStatus().name())")
    @Mapping(target = "serviceType", expression = "java(vehicle.getServiceType() != null ? vehicle.getServiceType().name() : null)")
    @Mapping(target = "availableUntil", expression = "java(vehicle.getAvailableUntil() != null ? vehicle.getAvailableUntil().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(vehicle.getCreatedAt() != null ? vehicle.getCreatedAt().toString() : null)")
    VehicleResponse toVehicleResponse(Vehicle vehicle);
}
