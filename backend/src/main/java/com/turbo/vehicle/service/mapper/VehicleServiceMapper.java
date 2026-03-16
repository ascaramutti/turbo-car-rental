package com.turbo.vehicle.service.mapper;

import com.turbo.user.model.CarOwner;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleServiceMapper {

    // ── Command → Entity mapping ────────────────────────────────────

    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "category", source = "command.category")
    @Mapping(target = "fuelType", source = "command.fuelType")
    @Mapping(target = "hourlyRate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "serviceType", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "availableUntil", ignore = true)
    @Mapping(target = "generalLocation", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Vehicle toEntity(RegisterVehicleCommand command, CarOwner owner);

    // ── Enum conversion helpers ─────────────────────────────────────

    /** Converts a category string to the VehicleCategory enum. */
    default VehicleCategory mapCategory(String category) {
        return VehicleCategory.valueOf(category.toUpperCase());
    }

    /** Converts a fuel type string to the FuelType enum. */
    default FuelType mapFuelType(String fuelType) {
        return FuelType.valueOf(fuelType.toUpperCase());
    }
}
