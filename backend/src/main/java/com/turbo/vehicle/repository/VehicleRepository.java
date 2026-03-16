package com.turbo.vehicle.repository;

import com.turbo.vehicle.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerUserId(Long ownerId);

    boolean existsByVin(String vin);

    boolean existsByLicensePlate(String licensePlate);
}
