package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverHoursSummaryResponse {

    private Integer hoursUsedThisWeek;
    private Integer maxHoursPerWeek;
    private Integer hoursRemaining;
}
