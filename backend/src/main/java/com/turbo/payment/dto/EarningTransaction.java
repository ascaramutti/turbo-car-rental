package com.turbo.payment.dto;

import com.turbo.payment.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EarningTransaction {

    private Long paymentId;
    private Long bookingId;
    private String vehicleSummary;
    private String driverFullName;
    private LocalDateTime date;
    private BigDecimal grossAmount;
    private BigDecimal platformFee;
    private BigDecimal ownerPayout;
    private PaymentStatus status;
}
