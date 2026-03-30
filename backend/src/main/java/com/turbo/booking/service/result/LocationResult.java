package com.turbo.booking.service.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain projection for vehicle location data with masking applied.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationResult {
    private Long vehicleId;
    private boolean exactLocation;
    private String generalLocation;
    private Double latitude;
    private Double longitude;
    private String message;
}
