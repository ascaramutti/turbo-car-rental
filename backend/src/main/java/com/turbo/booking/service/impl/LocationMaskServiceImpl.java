package com.turbo.booking.service.impl;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.service.LocationMaskService;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.validation.BookingValidationConstraints;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LocationMaskServiceImpl implements LocationMaskService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final String MESSAGE_LOCATION_NOT_YET_AVAILABLE =
            "Exact location will be available 2 hours before your booking";
    private static final String MESSAGE_LOCATION_NOW_AVAILABLE =
            "Exact pickup location is now available";

    @Override
    public double maskCoordinate(double coordinate) {
        return Math.round(coordinate * BookingValidationConstraints.MASK_PRECISION_FACTOR)
                / BookingValidationConstraints.MASK_PRECISION_FACTOR;
    }

    @Override
    public boolean shouldRevealExactLocation(Booking booking) {
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.COMPLETED) {
            return true;
        }
        if (status == BookingStatus.CONFIRMED || status == BookingStatus.IN_PROGRESS) {
            LocalDateTime revealThreshold = booking.getStartTime()
                    .minusHours(BookingValidationConstraints.LOCATION_REVEAL_HOURS);
            return !LocalDateTime.now().isBefore(revealThreshold);
        }
        return false;
    }

    @Override
    public LocationResult buildLocationResponse(Booking booking) {
        boolean exactLocation = shouldRevealExactLocation(booking);

        Double latitude;
        Double longitude;
        String message;

        if (exactLocation) {
            latitude = booking.getPickupLatitude();
            longitude = booking.getPickupLongitude();
            message = MESSAGE_LOCATION_NOW_AVAILABLE;
        } else {
            latitude = booking.getPickupLatitude() != null
                    ? maskCoordinate(booking.getPickupLatitude()) : null;
            longitude = booking.getPickupLongitude() != null
                    ? maskCoordinate(booking.getPickupLongitude()) : null;
            message = MESSAGE_LOCATION_NOT_YET_AVAILABLE;
        }

        return new LocationResult(
                booking.getVehicle().getVehicleId(),
                exactLocation,
                booking.getPickupLocation(),
                latitude,
                longitude,
                message
        );
    }

    @Override
    public boolean isWithinRadius(double vehicleLat, double vehicleLng,
                                  double searchLat, double searchLng, double radiusKm) {
        double distanceKm = calculateHaversineDistanceKm(vehicleLat, vehicleLng, searchLat, searchLng);
        return distanceKm <= radiusKm;
    }

    /** Calculates the great-circle distance between two points using the Haversine formula. */
    private double calculateHaversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLngRad = Math.toRadians(lng2 - lng1);

        double sinHalfDeltaLat = Math.sin(deltaLatRad / 2);
        double sinHalfDeltaLng = Math.sin(deltaLngRad / 2);

        double a = sinHalfDeltaLat * sinHalfDeltaLat
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * sinHalfDeltaLng * sinHalfDeltaLng;

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
