package com.turbo.booking.service.result;

import com.turbo.vehicle.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Domain projection for vehicle search results.
 * Carries the vehicle entity with computed fields for the controller mapper to convert into a DTO.
 */
@Getter
@AllArgsConstructor
public class VehicleSearchResult {
    private final Vehicle vehicle;
    private final double maskedLatitude;
    private final double maskedLongitude;
    private final String effectiveServiceType;
    private final String serviceTypeWarning;
}
