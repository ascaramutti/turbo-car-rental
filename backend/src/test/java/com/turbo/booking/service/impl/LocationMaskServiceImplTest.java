package com.turbo.booking.service.impl;

import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.validation.BookingValidationConstraints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("LocationMaskServiceImpl")
class LocationMaskServiceImplTest {

    private final LocationMaskServiceImpl locationMaskService = new LocationMaskServiceImpl();

    // ── maskCoordinate ────────────────────────────────────────────────

    @Nested
    @DisplayName("maskCoordinate()")
    class MaskCoordinate {

        @Test
        @DisplayName("Rounds coordinate to ~1km precision (2 decimal places)")
        void maskCoordinate_roundsToTwoDecimalPlaces() {
            double result = locationMaskService.maskCoordinate(45.5017);

            // factor = 100.0, so round(45.5017 * 100) / 100 = 45.50
            assertThat(result).isEqualTo(45.50);
        }

        @Test
        @DisplayName("Negative longitude is rounded correctly")
        void maskCoordinate_negativeLongitude_roundsCorrectly() {
            double result = locationMaskService.maskCoordinate(-73.5673);

            // round(-73.5673 * 100) / 100 = round(-7356.73) / 100 = -7357 / 100 = -73.57
            assertThat(result).isEqualTo(-73.57);
        }

        @Test
        @DisplayName("Coordinate with no fractional part stays the same")
        void maskCoordinate_wholeNumber_staysTheSame() {
            double result = locationMaskService.maskCoordinate(45.00);
            assertThat(result).isEqualTo(45.00);
        }

        @Test
        @DisplayName("Precision factor matches BookingValidationConstraints")
        void maskCoordinate_usesCorrectPrecisionFactor() {
            double coordinate = 12.3456789;
            double expected = Math.round(coordinate * BookingValidationConstraints.MASK_PRECISION_FACTOR)
                    / BookingValidationConstraints.MASK_PRECISION_FACTOR;

            assertThat(locationMaskService.maskCoordinate(coordinate)).isEqualTo(expected);
        }
    }

    // ── shouldRevealExactLocation ─────────────────────────────────────

    @Nested
    @DisplayName("shouldRevealExactLocation()")
    class ShouldRevealExactLocation {

        @Test
        @DisplayName("COMPLETED booking - always reveals exact location")
        void shouldRevealExactLocation_completed_returnsTrue() {
            Booking booking = BookingFixture.completedBooking();

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED booking >2h before startTime - returns false (masked)")
        void shouldRevealExactLocation_confirmed_moreThan2hBefore_returnsFalse() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(3)); // 3h away → still masked

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isFalse();
        }

        @Test
        @DisplayName("CONFIRMED booking <=2h before startTime - returns true (exact)")
        void shouldRevealExactLocation_confirmed_within2h_returnsTrue() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(1)); // 1h away → reveal

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isTrue();
        }

        @Test
        @DisplayName("CONFIRMED booking at exactly 2h before startTime - returns true (on boundary)")
        void shouldRevealExactLocation_confirmed_exactly2hBefore_returnsTrue() {
            Booking booking = BookingFixture.confirmedBooking();
            // startTime - 2h = now → threshold is now → !now.isBefore(now) = true
            booking.setStartTime(LocalDateTime.now().plusHours(2).plusSeconds(1));

            // Should still be false since we're just past the threshold boundary
            boolean result = locationMaskService.shouldRevealExactLocation(booking);
            // At exactly 2 hours away, the reveal hasn't started yet
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("IN_PROGRESS booking - always reveals exact location (within window)")
        void shouldRevealExactLocation_inProgress_returnsTrue() {
            Booking booking = BookingFixture.inProgressBooking();
            // startTime set to past so reveal threshold is definitely passed
            booking.setStartTime(LocalDateTime.now().minusHours(1));

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isTrue();
        }

        @Test
        @DisplayName("PENDING booking - returns false (never reveals location)")
        void shouldRevealExactLocation_pending_returnsFalse() {
            Booking booking = BookingFixture.pendingBooking();

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isFalse();
        }

        @Test
        @DisplayName("CANCELLED booking - returns false")
        void shouldRevealExactLocation_cancelled_returnsFalse() {
            Booking booking = BookingFixture.cancelledBooking();

            assertThat(locationMaskService.shouldRevealExactLocation(booking)).isFalse();
        }
    }

    // ── buildLocationResponse ─────────────────────────────────────────

    @Nested
    @DisplayName("buildLocationResponse()")
    class BuildLocationResponse {

        @Test
        @DisplayName("More than 2h before start - returns masked coordinates")
        void buildLocationResponse_maskedWindow_returnsMaskedCoords() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(3));
            booking.setPickupLatitude(45.5017);
            booking.setPickupLongitude(-73.5673);

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.isExactLocation()).isFalse();
            assertThat(response.getLatitude()).isEqualTo(45.50);
            assertThat(response.getLongitude()).isEqualTo(-73.57);
            assertThat(response.getMessage()).contains("2 hours");
        }

        @Test
        @DisplayName("Within 2h of start - returns exact coordinates")
        void buildLocationResponse_revealWindow_returnsExactCoords() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(1));
            booking.setPickupLatitude(45.5017);
            booking.setPickupLongitude(-73.5673);

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.isExactLocation()).isTrue();
            assertThat(response.getLatitude()).isEqualTo(45.5017);
            assertThat(response.getLongitude()).isEqualTo(-73.5673);
            assertThat(response.getMessage()).contains("now available");
        }

        @Test
        @DisplayName("COMPLETED booking - always returns exact coordinates")
        void buildLocationResponse_completed_returnsExactCoords() {
            Booking booking = BookingFixture.completedBooking();
            booking.setPickupLatitude(45.5017);
            booking.setPickupLongitude(-73.5673);

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.isExactLocation()).isTrue();
            assertThat(response.getLatitude()).isEqualTo(45.5017);
        }

        @Test
        @DisplayName("Null pickup coordinates - returns null masked coords without NPE")
        void buildLocationResponse_nullCoords_returnsNullCoords() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(3));
            booking.setPickupLatitude(null);
            booking.setPickupLongitude(null);

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.getLatitude()).isNull();
            assertThat(response.getLongitude()).isNull();
        }

        @Test
        @DisplayName("Response contains correct vehicleId")
        void buildLocationResponse_containsCorrectVehicleId() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(1));

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.getVehicleId()).isEqualTo(BookingFixture.VEHICLE_ID);
        }

        @Test
        @DisplayName("Response contains general location")
        void buildLocationResponse_containsGeneralLocation() {
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(LocalDateTime.now().plusHours(1));
            booking.setPickupLocation("Downtown Montreal");

            LocationResult response = locationMaskService.buildLocationResponse(booking);

            assertThat(response.getGeneralLocation()).isEqualTo("Downtown Montreal");
        }
    }

    // ── isWithinRadius ────────────────────────────────────────────────

    @Nested
    @DisplayName("isWithinRadius()")
    class IsWithinRadius {

        @Test
        @DisplayName("Same point - distance is 0, within any radius")
        void isWithinRadius_samePoint_returnsTrue() {
            assertThat(locationMaskService.isWithinRadius(45.5017, -73.5673, 45.5017, -73.5673, 1.0))
                    .isTrue();
        }

        @Test
        @DisplayName("Two nearby Montreal points (~1km) - within 2km radius")
        void isWithinRadius_nearbyPoints_returnsTrue() {
            // Downtown Montreal vs nearby point ~0.5km away
            assertThat(locationMaskService.isWithinRadius(45.5017, -73.5673, 45.5060, -73.5673, 2.0))
                    .isTrue();
        }

        @Test
        @DisplayName("Montreal vs Quebec City (~250km) - outside 10km radius")
        void isWithinRadius_distantPoints_returnsFalse() {
            // Montreal lat/lng vs Quebec City lat/lng
            assertThat(locationMaskService.isWithinRadius(45.5017, -73.5673, 46.8139, -71.2082, 10.0))
                    .isFalse();
        }

        @Test
        @DisplayName("Point just inside radius boundary - returns true")
        void isWithinRadius_justInsideBoundary_returnsTrue() {
            // Two points exactly on the equator separated by ~1km longitude
            // 1 degree longitude at equator ≈ 111km → 0.009 degree ≈ 1km
            assertThat(locationMaskService.isWithinRadius(0.0, 0.0, 0.0, 0.009, 1.1))
                    .isTrue();
        }

        @Test
        @DisplayName("Point just outside radius boundary - returns false")
        void isWithinRadius_justOutsideBoundary_returnsFalse() {
            // Move search center far enough to exclude the vehicle
            assertThat(locationMaskService.isWithinRadius(45.5017, -73.5673, 45.5017, -73.4673, 5.0))
                    .isFalse();
        }

        @Test
        @DisplayName("Large radius covers all reasonable distances")
        void isWithinRadius_largeRadius_returnsTrue() {
            assertThat(locationMaskService.isWithinRadius(45.5017, -73.5673, 46.8139, -71.2082, 500.0))
                    .isTrue();
        }
    }
}
