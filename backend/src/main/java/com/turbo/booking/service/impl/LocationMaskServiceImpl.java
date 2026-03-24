package com.turbo.booking.service.impl;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.service.LocationMaskService;
import com.turbo.booking.service.mapper.BookingServiceMapper;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.validation.BookingValidationConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LocationMaskServiceImpl implements LocationMaskService {

    private final BookingServiceMapper serviceMapper;

    private static final double EARTH_RADIUS_KM = 6371.0;

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
            message = BookingValidationConstraints.LOCATION_MESSAGE_NOW_AVAILABLE;
        } else {
            latitude = booking.getPickupLatitude() != null
                    ? maskCoordinate(booking.getPickupLatitude()) : null;
            longitude = booking.getPickupLongitude() != null
                    ? maskCoordinate(booking.getPickupLongitude()) : null;
            message = BookingValidationConstraints.LOCATION_MESSAGE_NOT_YET_AVAILABLE;
        }

        return serviceMapper.toLocationResult(
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
        double distanceKm = calculateDistanceKm(vehicleLat, vehicleLng, searchLat, searchLng);
        return distanceKm <= radiusKm;
    }

    @Override
    public double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
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
