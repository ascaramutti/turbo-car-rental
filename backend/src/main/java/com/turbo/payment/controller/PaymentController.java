package com.turbo.payment.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.payment.controller.mapper.PaymentControllerMapper;
import com.turbo.payment.dto.CreatePaymentIntentResponse;
import com.turbo.payment.dto.OwnerEarningsResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.service.PaymentService;
import com.turbo.user.model.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @PostMapping("/driver/bookings/{bookingId}/payments/intent")
    public ResponseEntity<CreatePaymentIntentResponse> createPaymentIntent(@PathVariable @Positive Long bookingId) {
        User user = securityHelper.getCurrentUser();
        CreatePaymentIntentResponse response = paymentService.createPaymentIntent(
                controllerMapper.toCreatePaymentIntentCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/driver/bookings/{bookingId}/payments/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable @Positive Long bookingId,
            @RequestParam @Pattern(regexp = "^pi_[a-zA-Z0-9]{1,255}$", message = "Invalid payment intent ID format") String paymentIntentId) {
        User user = securityHelper.getCurrentUser();
        PaymentResponse response = paymentService.confirmPayment(
                controllerMapper.toConfirmPaymentCommand(bookingId, user.getUserId(), paymentIntentId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{bookingId}/payments")
    public ResponseEntity<PaymentResponse> getBookingPayment(@PathVariable @Positive Long bookingId) {
        User user = securityHelper.getCurrentUser();
        PaymentResponse response = paymentService.getBookingPayment(
                controllerMapper.toGetPaymentCommand(bookingId, user.getUserId()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/owner/earnings")
    public ResponseEntity<OwnerEarningsResponse> getOwnerEarnings() {
        User user = securityHelper.getCurrentUser();
        OwnerEarningsResponse response = paymentService.getOwnerEarnings(user.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/stripe/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        paymentService.handleWebhookEvent(
                controllerMapper.toHandleWebhookCommand(payload, signatureHeader));
        return ResponseEntity.ok("Webhook processed");
    }
}
