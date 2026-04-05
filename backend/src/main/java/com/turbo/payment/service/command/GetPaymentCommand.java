package com.turbo.payment.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetPaymentCommand {

    private Long bookingId;
    private Long userId;
}
