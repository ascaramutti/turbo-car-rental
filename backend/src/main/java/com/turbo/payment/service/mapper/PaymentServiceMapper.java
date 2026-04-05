package com.turbo.payment.service.mapper;

import com.turbo.payment.dto.CreatePaymentIntentResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentServiceMapper {

    // ── CreatePaymentIntentResponse ─────────────────────────────────────

    @Mapping(source = "payment.paymentId", target = "paymentId")
    @Mapping(source = "paymentIntentId", target = "paymentIntentId")
    @Mapping(source = "clientSecret", target = "clientSecret")
    @Mapping(source = "payment.status", target = "status")
    @Mapping(source = "payment.amount", target = "amount")
    @Mapping(source = "payment.platformFee", target = "platformFee")
    @Mapping(source = "payment.ownerPayout", target = "ownerPayout")
    @Mapping(source = "payment.securityDeposit", target = "securityDeposit")
    @Mapping(source = "payment.currency", target = "currency")
    CreatePaymentIntentResponse toCreatePaymentIntentResponse(
            Payment payment, String paymentIntentId, String clientSecret);

    // ── PaymentResponse ─────────────────────────────────────────────────

    @Mapping(source = "paymentId", target = "paymentId")
    @Mapping(source = "booking.bookingId", target = "bookingId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "platformFee", target = "platformFee")
    @Mapping(source = "ownerPayout", target = "ownerPayout")
    @Mapping(source = "securityDeposit", target = "securityDeposit")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "stripePaymentIntentId", target = "stripePaymentIntentId")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    PaymentResponse toPaymentResponse(Payment payment);
}
