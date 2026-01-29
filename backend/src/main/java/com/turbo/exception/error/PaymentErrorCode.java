package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    BOOKING_NOT_FOUND("PAY-001", "Booking not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_PAYMENT_ACCESS("PAY-002", "You do not have permission to access this payment", HttpStatus.FORBIDDEN),
    BOOKING_NOT_CONFIRMABLE_FOR_PAYMENT("PAY-003", "Only confirmed bookings can be paid", HttpStatus.BAD_REQUEST),
    STRIPE_CONFIG_MISSING("PAY-004", "Payment processing is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    PAYMENT_ALREADY_COMPLETED("PAY-005", "Payment has already been completed for this booking", HttpStatus.CONFLICT),
    STRIPE_PAYMENT_INTENT_FAILED("PAY-006", "Failed to create payment intent", HttpStatus.BAD_GATEWAY),
    PAYMENT_NOT_FOUND("PAY-007", "Payment record not found for this booking", HttpStatus.NOT_FOUND),
    INVALID_STRIPE_WEBHOOK_SIGNATURE("PAY-008", "Invalid webhook signature", HttpStatus.BAD_REQUEST),
    STRIPE_WEBHOOK_PROCESSING_FAILED("PAY-009", "Failed to process payment webhook event", HttpStatus.INTERNAL_SERVER_ERROR),
    PAYMENT_REFUND_FAILED("PAY-010", "Failed to process payment refund", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
