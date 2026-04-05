package com.turbo.payment.dto;

import com.turbo.payment.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CreatePaymentIntentResponse {

    private Long paymentId;
    private String paymentIntentId;
    private String clientSecret;
    private PaymentStatus status;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal ownerPayout;
    private BigDecimal securityDeposit;
    private String currency;
}
