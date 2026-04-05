package com.turbo.payment.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPaymentCommand {

    private Long bookingId;
    private Long userId;
    private String paymentIntentId;
}
