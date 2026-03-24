package com.turbo.booking.service;

import com.turbo.booking.model.Booking;
import com.turbo.booking.service.result.LocationResult;

/**
 * Handles coordinate masking logic and the 2-hour location reveal window for bookings.
 */
public interface LocationMaskService {

    /**
     * Masks a latitude coordinate to approximately 1 km precision.
     *
     * @param latitude the exact latitude
     * @return the masked latitude
     */
    double maskCoordinate(double coordinate);

    /**
     * Returns true if the exact location should be revealed for this booking.
     * Exact location is revealed when the booking is CONFIRMED or IN_PROGRESS
     * and the start time is within 2 hours, or the booking is COMPLETED.
     *
     * @param booking the booking to evaluate
     * @return true if exact coordinates should be returned
     */
    boolean shouldRevealExactLocation(Booking booking);

    /**
     * Builds a LocationResult for a booking, applying masking rules.
     *
     * @param booking the confirmed/in-progress/completed booking
     * @return domain result with masked or exact coordinates and an informational message
     */
    LocationResult buildLocationResponse(Booking booking);

    /**
     * Returns whether the given vehicle is within the search radius from the driver's position.
     * Uses the Haversine formula.
     *
     * @param vehicleLat  vehicle latitude
     * @param vehicleLng  vehicle longitude
     * @param searchLat   driver search latitude
     * @param searchLng   driver search longitude
     * @param radiusKm    search radius in kilometres
     * @return true if within radius
     */
    boolean isWithinRadius(double vehicleLat, double vehicleLng,
                           double searchLat, double searchLng, double radiusKm);

    /**
     * Calculates the great-circle distance between two coordinates using the Haversine formula.
     *
     * @param lat1 first point latitude
     * @param lng1 first point longitude
     * @param lat2 second point latitude
     * @param lng2 second point longitude
     * @return distance in kilometres
     */
    double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2);
}
