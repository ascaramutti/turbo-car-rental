package com.turbo.vehicle.repository;

import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerUserId(Long ownerId);

    boolean existsByVin(String vin);

    boolean existsByLicensePlate(String licensePlate);

    /**
     * Returns active, approved vehicles that are still available and match the optional filters.
     * Geo-radius filtering is applied in-memory after this query using the Haversine formula.
     */
    @Query("SELECT v FROM Vehicle v WHERE v.isActive = true " +
           "AND v.status = 'APPROVED' " +
           "AND v.availableUntil > :now " +
           "AND (:category IS NULL OR v.category = :category) " +
           "AND (:serviceType IS NULL OR v.serviceType = :serviceType) " +
           "AND (:fuelType IS NULL OR v.fuelType = :fuelType) " +
           "AND (:minPrice IS NULL OR v.hourlyRate >= :minPrice) " +
           "AND (:maxPrice IS NULL OR v.hourlyRate <= :maxPrice)")
    List<Vehicle> searchAvailableVehicles(
            @Param("now") LocalDateTime now,
            @Param("category") VehicleCategory category,
            @Param("serviceType") ServiceType serviceType,
            @Param("fuelType") FuelType fuelType,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
