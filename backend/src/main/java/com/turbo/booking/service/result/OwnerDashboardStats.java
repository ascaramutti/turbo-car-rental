package com.turbo.booking.service.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Domain projection for an owner's dashboard statistics.
 * Returned by the service layer; converted to a DTO by the controller.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardStats {

    private int activeVehicles;
    private BigDecimal totalEarnings;
    private BigDecimal monthEarnings;
    private int completedBookings;
    private Float rating;
}
