package com.turbo.vehicle.fixture;

import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.vehicle.dto.ActivateVehicleRequest;
import com.turbo.vehicle.dto.RegisterVehicleRequest;
import com.turbo.vehicle.dto.VehicleResponse;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.model.enums.VehicleStatus;
import com.turbo.vehicle.service.command.ActivateVehicleCommand;
import com.turbo.vehicle.service.command.RegisterVehicleCommand;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class VehicleFixture {

    public static final Long OWNER_USER_ID = 20L;
    public static final Long VEHICLE_ID = 100L;

    private static final String TEST_VIN = "1HGBH41JXMN109186";
    private static final String TEST_MAKE = "Toyota";
    private static final String TEST_MODEL = "Corolla";
    private static final Integer TEST_YEAR = 2022;
    private static final String TEST_LICENSE_PLATE = "ABC 1234";
    private static final BigDecimal TEST_HOURLY_RATE = new BigDecimal("15.00");
    private static final String TEST_LOCATION = "Downtown Montreal";
    private static final Double TEST_LATITUDE = 45.5017;
    private static final Double TEST_LONGITUDE = -73.5673;

    private VehicleFixture() {}

    // ── Users ────────────────────────────────────────────────────────

    /** Creates a CarOwner with default test values. */
    public static CarOwner testCarOwner() {
        CarOwner owner = new CarOwner();
        owner.setUserId(OWNER_USER_ID);
        owner.setEmail("owner@test.com");
        owner.setFirstName("Sarah");
        owner.setLastName("Smith");
        owner.setRole(UserRole.CAR_OWNER);
        owner.setEmailVerified(true);
        owner.setRating(0.0f);
        return owner;
    }

    /** Creates a Driver (not a CarOwner — used for VEH-007 negative tests). */
    public static Driver testDriver() {
        Driver driver = new Driver();
        driver.setUserId(10L);
        driver.setEmail("driver@test.com");
        driver.setFirstName("John");
        driver.setLastName("Doe");
        driver.setRole(UserRole.DRIVER);
        driver.setEmailVerified(true);
        driver.setIsVerified(false);
        driver.setRating(0.0f);
        driver.setIsWorkEligible(false);
        return driver;
    }

    // ── Vehicles ─────────────────────────────────────────────────────

    /** Creates a Vehicle with status=PENDING and isActive=false. */
    public static Vehicle pendingVehicle() {
        return buildVehicle(VehicleStatus.PENDING, false);
    }

    /** Creates a Vehicle with status=APPROVED and isActive=false (approved but not listed). */
    public static Vehicle approvedVehicle() {
        return buildVehicle(VehicleStatus.APPROVED, false);
    }

    /** Creates a Vehicle with status=APPROVED and isActive=true (listed for rent). */
    public static Vehicle activeVehicle() {
        return buildVehicle(VehicleStatus.APPROVED, true);
    }

    /** Creates a pending Vehicle with a year older than the taxi-eligible age limit. */
    public static Vehicle oldPendingVehicle() {
        Vehicle vehicle = buildVehicle(VehicleStatus.PENDING, false);
        vehicle.setYear(2010);
        return vehicle;
    }

    // ── Request DTOs ────────────────────────────────────────────────

    /** Creates a valid RegisterVehicleRequest with all required fields. */
    public static RegisterVehicleRequest registerVehicleRequest() {
        RegisterVehicleRequest request = new RegisterVehicleRequest();
        request.setVin(TEST_VIN);
        request.setMake(TEST_MAKE);
        request.setModel(TEST_MODEL);
        request.setYear(TEST_YEAR);
        request.setLicensePlate(TEST_LICENSE_PLATE);
        request.setCategory("SEDAN");
        request.setFuelType("GASOLINE");
        request.setDescription("Well maintained vehicle");
        return request;
    }

    // ── Commands ─────────────────────────────────────────────────────

    /** Creates a valid RegisterVehicleCommand. */
    public static RegisterVehicleCommand registerVehicleCommand() {
        RegisterVehicleCommand command = new RegisterVehicleCommand();
        command.setOwnerId(OWNER_USER_ID);
        command.setVin(TEST_VIN);
        command.setMake(TEST_MAKE);
        command.setModel(TEST_MODEL);
        command.setYear(TEST_YEAR);
        command.setLicensePlate(TEST_LICENSE_PLATE);
        command.setCategory("SEDAN");
        command.setFuelType("GASOLINE");
        command.setDescription("Well maintained vehicle");
        return command;
    }

    // ── Response DTOs ───────────────────────────────────────────────

    /** Creates a VehicleResponse with PENDING status (hourlyRate is null since not yet activated). */
    public static VehicleResponse pendingVehicleResponse() {
        VehicleResponse response = new VehicleResponse();
        response.setVehicleId(VEHICLE_ID);
        response.setOwnerId(OWNER_USER_ID);
        response.setOwnerFullName("Sarah Smith");
        response.setVin(TEST_VIN);
        response.setMake(TEST_MAKE);
        response.setModel(TEST_MODEL);
        response.setYear(TEST_YEAR);
        response.setLicensePlate(TEST_LICENSE_PLATE);
        response.setCategory("SEDAN");
        response.setFuelType("GASOLINE");
        response.setHourlyRate(null);
        response.setDescription("Well maintained vehicle");
        response.setStatus("PENDING");
        response.setIsActive(false);
        return response;
    }

    // ── Activate fixtures ────────────────────────────────────────────

    /** Creates a valid ActivateVehicleRequest with location, hourlyRate, and future availableUntil. */
    public static ActivateVehicleRequest activateVehicleRequest() {
        ActivateVehicleRequest request = new ActivateVehicleRequest();
        request.setAvailableUntil(LocalDateTime.now().plusDays(7));
        request.setGeneralLocation(TEST_LOCATION);
        request.setLatitude(TEST_LATITUDE);
        request.setLongitude(TEST_LONGITUDE);
        request.setHourlyRate(TEST_HOURLY_RATE);
        return request;
    }

    /** Creates a valid ActivateVehicleCommand with location, hourlyRate, and future availableUntil. */
    public static ActivateVehicleCommand activateVehicleCommand() {
        ActivateVehicleCommand command = new ActivateVehicleCommand();
        command.setVehicleId(VEHICLE_ID);
        command.setOwnerId(OWNER_USER_ID);
        command.setAvailableUntil(LocalDateTime.now().plusDays(7));
        command.setGeneralLocation(TEST_LOCATION);
        command.setLatitude(TEST_LATITUDE);
        command.setLongitude(TEST_LONGITUDE);
        command.setHourlyRate(TEST_HOURLY_RATE);
        return command;
    }

    // ── Internal ─────────────────────────────────────────────────────

    private static Vehicle buildVehicle(VehicleStatus status, boolean isActive) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(VEHICLE_ID);
        vehicle.setOwner(testCarOwner());
        vehicle.setVin(TEST_VIN);
        vehicle.setMake(TEST_MAKE);
        vehicle.setModel(TEST_MODEL);
        vehicle.setYear(TEST_YEAR);
        vehicle.setLicensePlate(TEST_LICENSE_PLATE);
        vehicle.setCategory(VehicleCategory.SEDAN);
        vehicle.setFuelType(FuelType.GASOLINE);
        vehicle.setHourlyRate(isActive ? TEST_HOURLY_RATE : null);
        vehicle.setDescription("Well maintained vehicle");
        vehicle.setGeneralLocation(TEST_LOCATION);
        vehicle.setLatitude(TEST_LATITUDE);
        vehicle.setLongitude(TEST_LONGITUDE);
        vehicle.setStatus(status);
        vehicle.setIsActive(isActive);
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicle;
    }
}
