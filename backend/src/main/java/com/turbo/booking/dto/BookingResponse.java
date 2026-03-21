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
public class BookingResponse {

    private Long bookingId;
    private Long driverId;
    private String driverFullName;
    private Long vehicleId;
    private String vehicleSummary;
    private String status;
    private String startTime;
    private String endTime;
    private Integer totalHours;
    private BigDecimal totalPrice;
    private String createdAt;
    private String updatedAt;
}
