package com.turbo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardResponse {

    private Integer activeVehicles;
    private BigDecimal totalEarnings;
    private BigDecimal monthEarnings;
    private Integer completedBookings;
    private Float rating;
}
