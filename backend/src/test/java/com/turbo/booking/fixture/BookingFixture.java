package com.turbo.booking.fixture;

import com.turbo.booking.dto.AdminBookingDetailResponse;
import com.turbo.booking.dto.AdminBookingResponse;
import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.VehicleLocationResponse;
import com.turbo.booking.dto.VehicleSearchResponse;
import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.model.enums.VehicleStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Test fixture factory for Booking module tests.
 * Provides pre-built entities, commands, and response DTOs for all booking test scenarios.
 */
public final class BookingFixture {

    // ── Identity constants ────────────────────────────────────────────

    public static final Long DRIVER_ID      = 10L;
    public static final Long OWNER_ID       = 20L;
    public static final Long VEHICLE_ID     = 100L;
    public static final Long BOOKING_ID     = 200L;

    // ── Time constants ────────────────────────────────────────────────

    /** A start time well in the future for creating valid bookings. */
    public static final LocalDateTime FUTURE_START = LocalDateTime.now().plusDays(1);

    /** A valid end time 8 hours after FUTURE_START. */
    public static final LocalDateTime FUTURE_END   = FUTURE_START.plusHours(8);

    // ── Vehicle constants ─────────────────────────────────────────────

    private static final String TEST_VIN           = "1HGBH41JXMN109186";
    private static final String TEST_MAKE          = "Toyota";
    private static final String TEST_MODEL         = "Corolla";
    private static final Integer TEST_YEAR         = 2022;
    private static final String TEST_LICENSE_PLATE = "ABC 1234";
    private static final BigDecimal TEST_HOURLY_RATE = new BigDecimal("15.00");
    private static final String TEST_LOCATION      = "Downtown Montreal";
    private static final Double TEST_LATITUDE      = 45.5017;
    private static final Double TEST_LONGITUDE     = -73.5673;

    private BookingFixture() {}

    // ── Users ─────────────────────────────────────────────────────────

    /** Creates a verified Driver. */
    public static Driver verifiedDriver() {
        Driver driver = new Driver();
        driver.setUserId(DRIVER_ID);
        driver.setEmail("driver@test.com");
        driver.setFirstName("John");
        driver.setLastName("Doe");
        driver.setRole(UserRole.DRIVER);
        driver.setEmailVerified(true);
        driver.setIsVerified(true);
        driver.setRating(4.5f);
        driver.setIsWorkEligible(true);
        driver.setLicenseClass("CLASS_4");
        return driver;
    }

    /** Creates an unverified Driver (triggers BOOK-001). */
    public static Driver unverifiedDriver() {
        Driver driver = verifiedDriver();
        driver.setIsVerified(false);
        return driver;
    }

    /** Creates a Driver with CLASS_5 license (triggers service-type warning). */
    public static Driver class5Driver() {
        Driver driver = verifiedDriver();
        driver.setLicenseClass("CLASS_5");
        return driver;
    }

    /** Creates a CarOwner with default test values. */
    public static CarOwner testCarOwner() {
        CarOwner owner = new CarOwner();
        owner.setUserId(OWNER_ID);
        owner.setEmail("owner@test.com");
        owner.setFirstName("Sarah");
        owner.setLastName("Smith");
        owner.setRole(UserRole.CAR_OWNER);
        owner.setEmailVerified(true);
        owner.setRating(4.8f);
        return owner;
    }

    // ── Vehicles ──────────────────────────────────────────────────────

    /** Creates an active, approved vehicle available until far in the future. */
    public static Vehicle activeVehicle() {
        Vehicle vehicle = buildVehicle(VehicleStatus.APPROVED, true);
        vehicle.setAvailableUntil(LocalDateTime.now().plusDays(30));
        vehicle.setServiceType(ServiceType.TAXI_AND_DELIVERY);
        return vehicle;
    }

    /** Creates an approved but inactive vehicle (triggers BOOK-003). */
    public static Vehicle inactiveVehicle() {
        return buildVehicle(VehicleStatus.APPROVED, false);
    }

    /** Creates a pending (not approved) vehicle (triggers BOOK-002). */
    public static Vehicle pendingVehicle() {
        return buildVehicle(VehicleStatus.PENDING, false);
    }

    /** Creates an active vehicle owned by the driver (same userId — triggers BOOK-009). */
    public static Vehicle activeVehicleOwnedByDriver() {
        Vehicle vehicle = activeVehicle();
        CarOwner ownerAsDriver = new CarOwner();
        ownerAsDriver.setUserId(DRIVER_ID); // same as driver
        ownerAsDriver.setFirstName("John");
        ownerAsDriver.setLastName("Doe");
        ownerAsDriver.setRole(UserRole.CAR_OWNER);
        ownerAsDriver.setRating(0.0f);
        vehicle.setOwner(ownerAsDriver);
        return vehicle;
    }

    // ── Bookings ──────────────────────────────────────────────────────

    /** Creates a PENDING booking. */
    public static Booking pendingBooking() {
        return buildBooking(BookingStatus.PENDING, FUTURE_START, FUTURE_END);
    }

    /** Creates a CONFIRMED booking with pickup location set. */
    public static Booking confirmedBooking() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, FUTURE_START, FUTURE_END);
        booking.setConfirmedAt(LocalDateTime.now().minusHours(1));
        booking.setPickupLocation(TEST_LOCATION);
        booking.setPickupLatitude(TEST_LATITUDE);
        booking.setPickupLongitude(TEST_LONGITUDE);
        return booking;
    }

    /**
     * Creates a CONFIRMED booking whose start time is within the 15-minute start window.
     * Used for startBooking happy-path tests.
     */
    public static Booking confirmedBookingReadyToStart() {
        // startTime is 5 minutes in the future → within the 15-min early window
        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end   = start.plusHours(8);
        Booking booking = buildBooking(BookingStatus.CONFIRMED, start, end);
        booking.setConfirmedAt(LocalDateTime.now().minusHours(1));
        booking.setPickupLocation(TEST_LOCATION);
        booking.setPickupLatitude(TEST_LATITUDE);
        booking.setPickupLongitude(TEST_LONGITUDE);
        return booking;
    }

    /** Creates an IN_PROGRESS booking. */
    public static Booking inProgressBooking() {
        Booking booking = buildBooking(BookingStatus.IN_PROGRESS, FUTURE_START, FUTURE_END);
        booking.setConfirmedAt(LocalDateTime.now().minusHours(2));
        booking.setStartedAt(LocalDateTime.now().minusHours(1));
        booking.setPickupLocation(TEST_LOCATION);
        booking.setPickupLatitude(TEST_LATITUDE);
        booking.setPickupLongitude(TEST_LONGITUDE);
        return booking;
    }

    /** Creates a COMPLETED booking. */
    public static Booking completedBooking() {
        Booking booking = buildBooking(BookingStatus.COMPLETED,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(16));
        booking.setPickupLocation(TEST_LOCATION);
        booking.setPickupLatitude(TEST_LATITUDE);
        booking.setPickupLongitude(TEST_LONGITUDE);
        booking.setCompletedAt(LocalDateTime.now().minusHours(16));
        return booking;
    }

    /** Creates a CANCELLED booking. */
    public static Booking cancelledBooking() {
        Booking booking = buildBooking(BookingStatus.CANCELLED, FUTURE_START, FUTURE_END);
        booking.setCancellationReason("No longer needed");
        booking.setCancelledBy("DRIVER");
        booking.setCancelledAt(LocalDateTime.now());
        return booking;
    }

    // ── Commands ──────────────────────────────────────────────────────

    /** Creates a valid CreateBookingCommand. */
    public static CreateBookingCommand createBookingCommand() {
        CreateBookingCommand command = new CreateBookingCommand();
        command.setDriverId(DRIVER_ID);
        command.setVehicleId(VEHICLE_ID);
        command.setStartTime(FUTURE_START);
        command.setEndTime(FUTURE_END);
        return command;
    }

    /** Creates a SearchVehiclesCommand with no filters. */
    public static SearchVehiclesCommand searchVehiclesCommand() {
        SearchVehiclesCommand command = new SearchVehiclesCommand();
        command.setDriverId(DRIVER_ID);
        command.setRadiusKm(10.0);
        return command;
    }

    /** Creates a GetBookingCommand for a driver. */
    public static GetBookingCommand getBookingCommandForDriver() {
        GetBookingCommand command = new GetBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setUserId(DRIVER_ID);
        command.setUserRole(UserRole.DRIVER.name());
        return command;
    }

    /** Creates a GetBookingCommand for an owner. */
    public static GetBookingCommand getBookingCommandForOwner() {
        GetBookingCommand command = new GetBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setUserId(OWNER_ID);
        command.setUserRole(UserRole.CAR_OWNER.name());
        return command;
    }

    /** Creates a CancelBookingCommand for a driver. */
    public static CancelBookingCommand cancelCommandForDriver() {
        CancelBookingCommand command = new CancelBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setUserId(DRIVER_ID);
        command.setUserRole(UserRole.DRIVER.name());
        command.setReason("Plans changed");
        return command;
    }

    /** Creates a CancelBookingCommand for an owner. */
    public static CancelBookingCommand cancelCommandForOwner() {
        CancelBookingCommand command = new CancelBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setUserId(OWNER_ID);
        command.setUserRole(UserRole.CAR_OWNER.name());
        command.setReason("Vehicle under maintenance");
        return command;
    }

    /** Creates a ConfirmBookingCommand. */
    public static ConfirmBookingCommand confirmCommand() {
        ConfirmBookingCommand command = new ConfirmBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setOwnerId(OWNER_ID);
        return command;
    }

    /** Creates a RejectBookingCommand with a reason. */
    public static RejectBookingCommand rejectCommand() {
        RejectBookingCommand command = new RejectBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setOwnerId(OWNER_ID);
        command.setReason("Vehicle unavailable");
        return command;
    }

    /** Creates a StartBookingCommand with a single valid photo. */
    public static StartBookingCommand startCommand() {
        StartBookingCommand command = new StartBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setDriverId(DRIVER_ID);
        command.setPickupPhotos(List.of(validPhoto("pickupPhotos", "pickup.jpg")));
        return command;
    }

    /** Creates a CompleteBookingCommand with a single valid photo. */
    public static CompleteBookingCommand completeCommand() {
        CompleteBookingCommand command = new CompleteBookingCommand();
        command.setBookingId(BOOKING_ID);
        command.setDriverId(DRIVER_ID);
        command.setReturnPhotos(List.of(validPhoto("returnPhotos", "return.jpg")));
        return command;
    }

    // ── Response DTOs ─────────────────────────────────────────────────

    /** Creates a BookingResponse for a PENDING booking. */
    public static BookingResponse pendingBookingResponse() {
        BookingResponse response = new BookingResponse();
        response.setBookingId(BOOKING_ID);
        response.setDriverId(DRIVER_ID);
        response.setDriverFullName("John Doe");
        response.setVehicleId(VEHICLE_ID);
        response.setVehicleSummary("2022 Toyota Corolla");
        response.setStatus("PENDING");
        response.setStartTime(FUTURE_START.toString());
        response.setEndTime(FUTURE_END.toString());
        response.setTotalHours(8);
        response.setTotalPrice(new BigDecimal("120.00"));
        response.setCreatedAt(LocalDateTime.now().toString());
        response.setUpdatedAt(LocalDateTime.now().toString());
        return response;
    }

    /** Creates a BookingDetailResponse for a PENDING booking. */
    public static BookingDetailResponse pendingBookingDetailResponse() {
        BookingDetailResponse response = new BookingDetailResponse();
        response.setBookingId(BOOKING_ID);
        response.setDriverId(DRIVER_ID);
        response.setDriverFullName("John Doe");
        response.setVehicleId(VEHICLE_ID);
        response.setVehicleSummary("2022 Toyota Corolla");
        response.setStatus("PENDING");
        response.setStartTime(FUTURE_START.toString());
        response.setEndTime(FUTURE_END.toString());
        response.setTotalHours(8);
        response.setTotalPrice(new BigDecimal("120.00"));
        response.setOwnerId(OWNER_ID);
        response.setOwnerFullName("Sarah Smith");
        response.setVehicleMake(TEST_MAKE);
        response.setVehicleModel(TEST_MODEL);
        response.setVehicleYear(TEST_YEAR);
        response.setVehicleLicensePlate(TEST_LICENSE_PLATE);
        response.setVehicleCategory("SEDAN");
        response.setVehicleHourlyRate(TEST_HOURLY_RATE);
        response.setCreatedAt(LocalDateTime.now().toString());
        response.setUpdatedAt(LocalDateTime.now().toString());
        return response;
    }

    /** Creates an AdminBookingResponse. */
    public static AdminBookingResponse adminBookingResponse() {
        AdminBookingResponse response = new AdminBookingResponse();
        response.setBookingId(BOOKING_ID);
        response.setDriverId(DRIVER_ID);
        response.setDriverFullName("John Doe");
        response.setVehicleId(VEHICLE_ID);
        response.setVehicleSummary("2022 Toyota Corolla");
        response.setStatus("PENDING");
        response.setStartTime(FUTURE_START.toString());
        response.setEndTime(FUTURE_END.toString());
        response.setOwnerId(OWNER_ID);
        response.setOwnerFullName("Sarah Smith");
        response.setVehicleLicensePlate(TEST_LICENSE_PLATE);
        response.setCreatedAt(LocalDateTime.now().toString());
        response.setUpdatedAt(LocalDateTime.now().toString());
        return response;
    }

    /** Creates an AdminBookingDetailResponse. */
    public static AdminBookingDetailResponse adminBookingDetailResponse() {
        AdminBookingDetailResponse response = new AdminBookingDetailResponse();
        response.setBookingId(BOOKING_ID);
        response.setDriverId(DRIVER_ID);
        response.setDriverFullName("John Doe");
        response.setVehicleId(VEHICLE_ID);
        response.setVehicleSummary("2022 Toyota Corolla");
        response.setStatus("PENDING");
        response.setStartTime(FUTURE_START.toString());
        response.setEndTime(FUTURE_END.toString());
        response.setOwnerId(OWNER_ID);
        response.setOwnerFullName("Sarah Smith");
        response.setVehicleLicensePlate(TEST_LICENSE_PLATE);
        response.setVehicleMake(TEST_MAKE);
        response.setVehicleModel(TEST_MODEL);
        response.setVehicleYear(TEST_YEAR);
        response.setVehicleCategory("SEDAN");
        response.setVehicleHourlyRate(TEST_HOURLY_RATE);
        response.setCreatedAt(LocalDateTime.now().toString());
        response.setUpdatedAt(LocalDateTime.now().toString());
        return response;
    }

    /** Creates a VehicleSearchResponse. */
    public static VehicleSearchResponse vehicleSearchResponse() {
        VehicleSearchResponse response = new VehicleSearchResponse();
        response.setVehicleId(VEHICLE_ID);
        response.setMake(TEST_MAKE);
        response.setModel(TEST_MODEL);
        response.setYear(TEST_YEAR);
        response.setCategory("SEDAN");
        response.setFuelType("GASOLINE");
        response.setServiceType("TAXI_AND_DELIVERY");
        response.setHourlyRate(TEST_HOURLY_RATE);
        response.setGeneralLocation(TEST_LOCATION);
        response.setMaskedLatitude(45.50);
        response.setMaskedLongitude(-73.57);
        response.setOwnerFullName("Sarah Smith");
        response.setOwnerRating(4.8f);
        response.setEffectiveServiceType("TAXI_AND_DELIVERY");
        return response;
    }

    /** Creates a VehicleLocationResponse (masked). */
    public static VehicleLocationResponse maskedLocationResponse() {
        return new VehicleLocationResponse(
                VEHICLE_ID,
                false,
                TEST_LOCATION,
                45.50,
                -73.57,
                "Exact location will be available 2 hours before your booking"
        );
    }

    /** Creates a VehicleLocationResponse (exact). */
    public static VehicleLocationResponse exactLocationResponse() {
        return new VehicleLocationResponse(
                VEHICLE_ID,
                true,
                TEST_LOCATION,
                TEST_LATITUDE,
                TEST_LONGITUDE,
                "Exact pickup location is now available"
        );
    }

    /** Creates a VehicleSearchResult domain object for the active vehicle with CLASS_4 driver. */
    public static VehicleSearchResult vehicleSearchResult() {
        return new VehicleSearchResult(
                activeVehicle(),
                45.50,
                -73.57,
                "TAXI_AND_DELIVERY",
                null
        );
    }

    /** Creates a LocationResult (masked) for a confirmed booking. */
    public static LocationResult maskedLocationResult() {
        return new LocationResult(
                VEHICLE_ID,
                false,
                TEST_LOCATION,
                45.50,
                -73.57,
                "Exact location will be available 2 hours before your booking"
        );
    }

    /** Creates a LocationResult (exact) for a booking within the reveal window. */
    public static LocationResult exactLocationResult() {
        return new LocationResult(
                VEHICLE_ID,
                true,
                TEST_LOCATION,
                TEST_LATITUDE,
                TEST_LONGITUDE,
                "Exact pickup location is now available"
        );
    }

    // ── Files ─────────────────────────────────────────────────────────

    /** Creates a valid JPEG MockMultipartFile for photo upload tests. */
    public static MockMultipartFile validPhoto(String paramName, String fileName) {
        return new MockMultipartFile(paramName, fileName, "image/jpeg", "photo-content".getBytes());
    }

    /** Creates an oversized photo (>5MB) to trigger file-size validation. */
    public static MockMultipartFile oversizedPhoto(String paramName) {
        byte[] content = new byte[6 * 1024 * 1024];
        return new MockMultipartFile(paramName, "large.jpg", "image/jpeg", content);
    }

    /** Creates a photo with an invalid extension (triggers DOC-001). */
    public static MockMultipartFile invalidFormatPhoto(String paramName) {
        return new MockMultipartFile(paramName, "doc.docx", "application/msword", "doc-content".getBytes());
    }

    // ── Internal helpers ──────────────────────────────────────────────

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

    private static Booking buildBooking(BookingStatus status, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setBookingId(BOOKING_ID);
        booking.setDriver(verifiedDriver());
        booking.setVehicle(activeVehicle());
        booking.setStatus(status);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setTotalHours(8);
        booking.setTotalPrice(new BigDecimal("120.00"));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return booking;
    }
}
