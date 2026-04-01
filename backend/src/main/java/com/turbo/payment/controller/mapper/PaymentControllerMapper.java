package com.turbo.payment.controller.mapper;

import com.turbo.payment.service.command.ConfirmPaymentCommand;
import com.turbo.payment.service.command.CreatePaymentIntentCommand;
import com.turbo.payment.service.command.GetPaymentCommand;
import com.turbo.payment.service.command.HandleWebhookCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentControllerMapper {

    CreatePaymentIntentCommand toCreatePaymentIntentCommand(Long bookingId, Long userId);

    ConfirmPaymentCommand toConfirmPaymentCommand(Long bookingId, Long userId, String paymentIntentId);

    GetPaymentCommand toGetPaymentCommand(Long bookingId, Long userId);

    HandleWebhookCommand toHandleWebhookCommand(String payload, String stripeSignatureHeader);
}
