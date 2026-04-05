package com.turbo.payment.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandleWebhookCommand {

    private String payload;
    private String stripeSignatureHeader;
}
