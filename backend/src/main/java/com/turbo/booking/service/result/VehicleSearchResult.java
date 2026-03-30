package com.turbo.booking.service.result;

import com.turbo.vehicle.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain projection for vehicle search results.
 * Carries the vehicle entity with computed fields for the controller mapper to convert into a DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSearchResult {
    private Vehicle vehicle;
    private Double maskedLatitude;
    private Double maskedLongitude;
    private String effectiveServiceType;
    private String serviceTypeWarning;
}
