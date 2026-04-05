package com.turbo.payment.service;

import com.turbo.booking.model.Booking;
import com.turbo.payment.dto.CreatePaymentIntentResponse;
import com.turbo.payment.dto.OwnerEarningsResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.service.command.CreatePaymentIntentCommand;
import com.turbo.payment.service.command.GetPaymentCommand;
import com.turbo.payment.service.command.ConfirmPaymentCommand;
import com.turbo.payment.service.command.HandleWebhookCommand;

public interface PaymentService {

    CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentCommand command);

    PaymentResponse confirmPayment(ConfirmPaymentCommand command);

    PaymentResponse getBookingPayment(GetPaymentCommand command);

    void handleWebhookEvent(HandleWebhookCommand command);

    void refundPaymentForCancelledBooking(Booking booking);

    OwnerEarningsResponse getOwnerEarnings(Long ownerId);
}
