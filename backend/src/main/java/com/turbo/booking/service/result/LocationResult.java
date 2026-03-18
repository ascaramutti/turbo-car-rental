package com.turbo.booking.service.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain projection for vehicle location data with masking applied.
 */
@Getter
@AllArgsConstructor
public class LocationResult {
    private final Long vehicleId;
    private final boolean exactLocation;
    private final String generalLocation;
    private final Double latitude;
    private final Double longitude;
    private final String message;
}
