package com.turbo.booking.service.mapper;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.model.enums.VehicleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BookingServiceMapper}.
 * Uses {@code Mappers.getMapper()} — this is the one allowed place for direct mapper instantiation.
 */
@DisplayName("BookingServiceMapper")
class BookingServiceMapperTest {

    private final BookingServiceMapper mapper = Mappers.getMapper(BookingServiceMapper.class);

    // ── Test data helpers ─────────────────────────────────────────────

    private static final Long DRIVER_ID  = 10L;
    private static final Long VEHICLE_ID = 100L;
    private static final Long BOOKING_ID = 200L;
    private static final Long OWNER_ID   = 20L;

    private Driver buildDriver() {
        Driver driver = new Driver();
        driver.setUserId(DRIVER_ID);
        driver.setFirstName("John");
        driver.setLastName("Doe");
        driver.setRole(UserRole.DRIVER);
        driver.setIsVerified(true);
        return driver;
    }

    private Vehicle buildVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(VEHICLE_ID);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(2022);
        vehicle.setStatus(VehicleStatus.APPROVED);
        vehicle.setIsActive(true);
        vehicle.setCategory(VehicleCategory.SEDAN);
        vehicle.setFuelType(FuelType.GASOLINE);
        vehicle.setHourlyRate(new BigDecimal("15.00"));
        return vehicle;
    }

    private CreateBookingCommand buildCommand(LocalDateTime start, LocalDateTime end) {
        CreateBookingCommand command = new CreateBookingCommand();
        command.setDriverId(DRIVER_ID);
        command.setVehicleId(VEHICLE_ID);
        command.setStartTime(start);
        command.setEndTime(end);
        return command;
    }

    // ── toBooking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("toBooking()")
    class ToBooking {

        @Test
        @DisplayName("Maps all fields correctly from command and resolved entities")
        void toBooking_mapsAllFields() {
            LocalDateTime start = LocalDateTime.now().plusDays(1);
            LocalDateTime end   = start.plusHours(8);
            Driver driver   = buildDriver();
            Vehicle vehicle = buildVehicle();
            BigDecimal price = new BigDecimal("120.00");

            Booking result = mapper.toBooking(buildCommand(start, end), driver, vehicle, 8, price);

            assertThat(result).isNotNull();
            assertThat(result.getDriver()).isEqualTo(driver);
            assertThat(result.getVehicle()).isEqualTo(vehicle);
            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(result.getStartTime()).isEqualTo(start);
            assertThat(result.getEndTime()).isEqualTo(end);
            assertThat(result.getTotalHours()).isEqualTo(8);
            assertThat(result.getTotalPrice()).isEqualByComparingTo("120.00");
        }

        @Test
        @DisplayName("Status is always PENDING regardless of other inputs")
        void toBooking_statusIsAlwaysPending() {
            LocalDateTime start = LocalDateTime.now().plusDays(1);
            Driver driver = buildDriver();
            Vehicle vehicle = buildVehicle();

            Booking result = mapper.toBooking(buildCommand(start, start.plusHours(4)), driver, vehicle, 4, BigDecimal.TEN);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        @DisplayName("Driver and vehicle references are preserved as-is")
        void toBooking_preservesEntityReferences() {
            Driver driver = buildDriver();
            Vehicle vehicle = buildVehicle();
            LocalDateTime start = LocalDateTime.now().plusDays(1);

            Booking result = mapper.toBooking(buildCommand(start, start.plusHours(8)), driver, vehicle, 8, BigDecimal.ONE);

            assertThat(result.getDriver()).isSameAs(driver);
            assertThat(result.getVehicle()).isSameAs(vehicle);
        }
    }

    // ── toBookingPhoto ────────────────────────────────────────────────

    @Nested
    @DisplayName("toBookingPhoto()")
    class ToBookingPhoto {

        @Test
        @DisplayName("Maps all photo fields correctly")
        void toBookingPhoto_mapsAllFields() {
            Booking booking = new Booking();
            booking.setBookingId(BOOKING_ID);

            BookingPhoto result = mapper.toBookingPhoto(
                    booking,
                    BookingValidationConstraints.PHOTO_TYPE_PICKUP,
                    "bookings/200/pickup/photo.jpg",
                    "photo.jpg",
                    512_000L);

            assertThat(result).isNotNull();
            assertThat(result.getBooking()).isSameAs(booking);
            assertThat(result.getPhotoType()).isEqualTo(BookingValidationConstraints.PHOTO_TYPE_PICKUP);
            assertThat(result.getFileUrl()).isEqualTo("bookings/200/pickup/photo.jpg");
            assertThat(result.getFileName()).isEqualTo("photo.jpg");
            assertThat(result.getFileSize()).isEqualTo(512_000L);
        }

        @Test
        @DisplayName("RETURN photo type is set correctly")
        void toBookingPhoto_returnPhotoType_isSet() {
            Booking booking = new Booking();

            BookingPhoto result = mapper.toBookingPhoto(
                    booking,
                    BookingValidationConstraints.PHOTO_TYPE_RETURN,
                    "bookings/200/return/photo.jpg",
                    "return.jpg",
                    1_024_000L);

            assertThat(result.getPhotoType()).isEqualTo(BookingValidationConstraints.PHOTO_TYPE_RETURN);
        }
    }

    // ── toVehicleSearchResult ─────────────────────────────────────────

    @Nested
    @DisplayName("toVehicleSearchResult()")
    class ToVehicleSearchResult {

        @Test
        @DisplayName("Maps vehicle entity and computed fields into the result")
        void toVehicleSearchResult_mapsAllFields() {
            Vehicle vehicle = buildVehicle();

            VehicleSearchResult result = mapper.toVehicleSearchResult(
                    vehicle, 45.50, -73.57, "DELIVERY_ONLY", "CLASS_5 warning", null);

            assertThat(result.getVehicle()).isSameAs(vehicle);
            assertThat(result.getMaskedLatitude()).isEqualTo(45.50);
            assertThat(result.getMaskedLongitude()).isEqualTo(-73.57);
            assertThat(result.getEffectiveServiceType()).isEqualTo("DELIVERY_ONLY");
            assertThat(result.getServiceTypeWarning()).isEqualTo("CLASS_5 warning");
        }

        @Test
        @DisplayName("Null serviceTypeWarning is preserved as null")
        void toVehicleSearchResult_noWarning_isNull() {
            Vehicle vehicle = buildVehicle();

            VehicleSearchResult result = mapper.toVehicleSearchResult(vehicle, 0.0, 0.0, "TAXI_AND_DELIVERY", null, null);

            assertThat(result.getServiceTypeWarning()).isNull();
        }
    }

    // ── toLocationResult ──────────────────────────────────────────────

    @Nested
    @DisplayName("toLocationResult()")
    class ToLocationResult {

        @Test
        @DisplayName("Maps all location fields correctly (exact location)")
        void toLocationResult_exactLocation_mapsAllFields() {
            LocationResult result = mapper.toLocationResult(
                    VEHICLE_ID, true, "Downtown Montreal", 45.5017, -73.5673, "Exact pickup location is now available");

            assertThat(result.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(result.isExactLocation()).isTrue();
            assertThat(result.getGeneralLocation()).isEqualTo("Downtown Montreal");
            assertThat(result.getLatitude()).isEqualTo(45.5017);
            assertThat(result.getLongitude()).isEqualTo(-73.5673);
            assertThat(result.getMessage()).contains("now available");
        }

        @Test
        @DisplayName("Maps all location fields correctly (masked location)")
        void toLocationResult_maskedLocation_mapsAllFields() {
            LocationResult result = mapper.toLocationResult(
                    VEHICLE_ID, false, "Downtown Montreal", 45.50, -73.57,
                    "Exact location will be available 2 hours before your booking");

            assertThat(result.isExactLocation()).isFalse();
            assertThat(result.getLatitude()).isEqualTo(45.50);
            assertThat(result.getLongitude()).isEqualTo(-73.57);
            assertThat(result.getMessage()).contains("2 hours");
        }

        @Test
        @DisplayName("Null coordinates are preserved as null")
        void toLocationResult_nullCoords_areNull() {
            LocationResult result = mapper.toLocationResult(
                    VEHICLE_ID, false, "Unknown", null, null, "Location not available");

            assertThat(result.getLatitude()).isNull();
            assertThat(result.getLongitude()).isNull();
        }
    }

    // ── toDriverHoursSummary ──────────────────────────────────────────

    @Nested
    @DisplayName("toDriverHoursSummary()")
    class ToDriverHoursSummary {

        @Test
        @DisplayName("Maps hours used, max, and remaining correctly")
        void toDriverHoursSummary_mapsAllFields() {
            int max = BookingValidationConstraints.MAX_SHIFT_HOURS;

            DriverHoursSummary result = mapper.toDriverHoursSummary(8, max, max - 8);

            assertThat(result.getHoursUsedThisWeek()).isEqualTo(8);
            assertThat(result.getMaxHoursPerWeek()).isEqualTo(max);
            assertThat(result.getHoursRemaining()).isEqualTo(max - 8);
        }

        @Test
        @DisplayName("Zero hours used - remaining equals max")
        void toDriverHoursSummary_zeroUsed_remainingIsMax() {
            int max = BookingValidationConstraints.MAX_SHIFT_HOURS;

            DriverHoursSummary result = mapper.toDriverHoursSummary(0, max, max);

            assertThat(result.getHoursUsedThisWeek()).isZero();
            assertThat(result.getHoursRemaining()).isEqualTo(max);
        }

        @Test
        @DisplayName("Max hours used - remaining is 0")
        void toDriverHoursSummary_maxUsed_remainingIsZero() {
            int max = BookingValidationConstraints.MAX_SHIFT_HOURS;

            DriverHoursSummary result = mapper.toDriverHoursSummary(max, max, 0);

            assertThat(result.getHoursUsedThisWeek()).isEqualTo(max);
            assertThat(result.getHoursRemaining()).isZero();
        }
    }

    // ── toOwnerDashboardStats ─────────────────────────────────────────

    @Nested
    @DisplayName("toOwnerDashboardStats()")
    class ToOwnerDashboardStats {

        @Test
        @DisplayName("Maps all stats fields correctly")
        void toOwnerDashboardStats_mapsAllFields() {
            BigDecimal totalEarnings = new BigDecimal("960.00");
            BigDecimal monthEarnings = new BigDecimal("120.00");

            OwnerDashboardStats result = mapper.toOwnerDashboardStats(3, totalEarnings, monthEarnings, 8, 4.8f);

            assertThat(result.getActiveVehicles()).isEqualTo(3);
            assertThat(result.getTotalEarnings()).isEqualByComparingTo("960.00");
            assertThat(result.getMonthEarnings()).isEqualByComparingTo("120.00");
            assertThat(result.getCompletedBookings()).isEqualTo(8);
            assertThat(result.getRating()).isEqualTo(4.8f);
        }

        @Test
        @DisplayName("Null rating is preserved as null (owner not yet rated)")
        void toOwnerDashboardStats_nullRating_isNull() {
            OwnerDashboardStats result = mapper.toOwnerDashboardStats(
                    0, BigDecimal.ZERO, BigDecimal.ZERO, 0, null);

            assertThat(result.getRating()).isNull();
        }

        @Test
        @DisplayName("Zero earnings and bookings are mapped correctly")
        void toOwnerDashboardStats_zeroValues_mappedCorrectly() {
            OwnerDashboardStats result = mapper.toOwnerDashboardStats(
                    0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0.0f);

            assertThat(result.getActiveVehicles()).isZero();
            assertThat(result.getTotalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getMonthEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getCompletedBookings()).isZero();
        }
    }
}
