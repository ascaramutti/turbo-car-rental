package com.turbo.booking.service.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain projection for a driver's weekly hours usage summary.
 * Returned by the service layer; converted to a DTO by the controller mapper.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverHoursSummary {

    private int hoursUsedThisWeek;
    private int maxHoursPerWeek;
    private int hoursRemaining;
}
