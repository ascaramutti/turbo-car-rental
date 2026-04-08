package com.turbo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class OwnerEarningsResponse {

    private BigDecimal totalEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal pendingPayouts;
    private int completedPayments;
    private List<EarningTransaction> transactions;
}
