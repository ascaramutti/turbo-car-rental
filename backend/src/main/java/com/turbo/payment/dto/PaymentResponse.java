package com.turbo.payment.dto;

import com.turbo.payment.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long bookingId;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal ownerPayout;
    private BigDecimal securityDeposit;
    private String currency;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
