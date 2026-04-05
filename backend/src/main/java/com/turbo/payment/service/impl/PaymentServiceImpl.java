package com.turbo.payment.service.impl;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.PaymentErrorCode;
import com.turbo.payment.dto.CreatePaymentIntentResponse;
import com.turbo.payment.dto.EarningTransaction;
import com.turbo.payment.dto.OwnerEarningsResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.model.Payment;
import com.turbo.payment.model.enums.PaymentStatus;
import com.turbo.payment.repository.PaymentRepository;
import com.turbo.payment.service.PaymentService;
import com.turbo.payment.service.command.ConfirmPaymentCommand;
import com.turbo.payment.service.command.CreatePaymentIntentCommand;
import com.turbo.payment.service.command.GetPaymentCommand;
import com.turbo.payment.service.command.HandleWebhookCommand;
import com.turbo.payment.service.mapper.PaymentServiceMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal CENTS_MULTIPLIER = new BigDecimal("100");

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentServiceMapper serviceMapper;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${stripe.currency:cad}")
    private String currency;

    @Value("${app.payment.security-deposit:200.00}")
    private BigDecimal securityDeposit;

    /** Initializes the Stripe API key on startup if configured. */
    @PostConstruct
    void initStripe() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    // ── Create payment intent ───────────────────────────────────────────

    @Override
    @Transactional
    public CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToDriver(booking, command.getUserId());
        validateBookingIsConfirmed(booking);
        validateStripeConfigured();

        Payment payment = paymentRepository.findByBookingBookingId(command.getBookingId())
                .orElse(null);
        if (payment != null) {
            validatePaymentNotAlreadyCompleted(payment);
        } else {
            payment = new Payment();
        }

        BigDecimal baseAmount = normalizeAmount(booking.getTotalPrice());
        BigDecimal normalizedDeposit = normalizeAmount(securityDeposit);
        BigDecimal ownerPayout = booking.getVehicle().getHourlyRate()
                .multiply(BigDecimal.valueOf(booking.getTotalHours()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal platformFee = baseAmount.subtract(ownerPayout)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalAmount = baseAmount.add(normalizedDeposit).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        try {
            PaymentIntent intent = createStripePaymentIntent(command.getBookingId(), totalAmount);

            payment.setBooking(booking);
            payment.setAmount(totalAmount);
            payment.setPlatformFee(platformFee);
            payment.setOwnerPayout(ownerPayout);
            payment.setSecurityDeposit(normalizedDeposit);
            payment.setCurrency(currency.toLowerCase());
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus(PaymentStatus.PENDING);

            Payment saved = paymentRepository.save(payment);

            return serviceMapper.toCreatePaymentIntentResponse(
                    saved, intent.getId(), intent.getClientSecret());
        } catch (StripeException ex) {
            log.error("Could not create Stripe payment intent for booking {}", command.getBookingId(), ex);
            throw new BusinessException(PaymentErrorCode.STRIPE_PAYMENT_INTENT_FAILED);
        }
    }

    // ── Confirm payment (called by frontend after Stripe confirms) ────

    @Override
    @Transactional
    public PaymentResponse confirmPayment(ConfirmPaymentCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToDriver(booking, command.getUserId());

        Payment payment = paymentRepository.findByBookingBookingId(command.getBookingId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return serviceMapper.toPaymentResponse(payment);
        }

        if (!payment.getStripePaymentIntentId().equals(command.getPaymentIntentId())) {
            throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        Payment saved = paymentRepository.save(payment);
        return serviceMapper.toPaymentResponse(saved);
    }

    // ── Get payment for booking ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getBookingPayment(GetPaymentCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateUserHasPaymentAccess(booking, command.getUserId());

        Payment payment = paymentRepository.findByBookingBookingId(command.getBookingId())
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return serviceMapper.toPaymentResponse(payment);
    }

    // ── Stripe webhook ──────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhookEvent(HandleWebhookCommand command) {
        validateWebhookSecretConfigured();

        final Event event;
        try {
            event = Webhook.constructEvent(command.getPayload(), command.getStripeSignatureHeader(), stripeWebhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new BusinessException(PaymentErrorCode.INVALID_STRIPE_WEBHOOK_SIGNATURE);
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent pi = deserializePaymentIntent(event);
                    updatePaymentStatus(pi.getId(), PaymentStatus.COMPLETED);
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = deserializePaymentIntent(event);
                    updatePaymentStatus(pi.getId(), PaymentStatus.FAILED);
                }
                default -> log.info("Ignoring unsupported Stripe event type: {}", event.getType());
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Could not process Stripe webhook event {}", event.getType(), ex);
            throw new BusinessException(PaymentErrorCode.STRIPE_WEBHOOK_PROCESSING_FAILED);
        }
    }

    // ── Refund on cancellation ──────────────────────────────────────────

    @Override
    @Transactional
    public void refundPaymentForCancelledBooking(Booking booking) {
        paymentRepository.findByBookingBookingId(booking.getBookingId()).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.COMPLETED || payment.getStripePaymentIntentId() == null) {
                return;
            }
            try {
                Refund.create(RefundCreateParams.builder()
                        .setPaymentIntent(payment.getStripePaymentIntentId())
                        .build());
            } catch (StripeException ex) {
                log.warn("Stripe refund skipped for booking {} — no successful charge to refund",
                        booking.getBookingId());
            }
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        });
    }

    // ── Owner earnings ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerEarningsResponse getOwnerEarnings(Long ownerId) {
        BigDecimal totalEarnings = paymentRepository.sumOwnerPayoutByStatus(ownerId, PaymentStatus.COMPLETED);
        BigDecimal pendingPayouts = paymentRepository.sumOwnerPayoutByStatus(ownerId, PaymentStatus.PENDING);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        BigDecimal monthEarnings = paymentRepository.sumOwnerPayoutInRange(ownerId, monthStart, monthEnd);

        long completedCount = paymentRepository.countByOwnerAndStatus(ownerId, PaymentStatus.COMPLETED);

        List<Payment> payments = paymentRepository.findByOwnerOrderByCreatedAtDesc(ownerId);
        List<EarningTransaction> transactions = payments.stream()
                .map(this::toEarningTransaction)
                .toList();

        return new OwnerEarningsResponse(totalEarnings, monthEarnings, pendingPayouts,
                (int) completedCount, transactions);
    }

    private EarningTransaction toEarningTransaction(Payment payment) {
        var booking = payment.getBooking();
        var vehicle = booking.getVehicle();
        var driver = booking.getDriver();

        String vehicleSummary = vehicle.getYear() + " " + vehicle.getMake() + " " + vehicle.getModel();
        String driverFullName = driver.getFirstName() + " " + driver.getLastName();
        BigDecimal grossAmount = payment.getAmount().subtract(payment.getSecurityDeposit());

        return new EarningTransaction(
                payment.getPaymentId(), booking.getBookingId(), vehicleSummary,
                driverFullName, payment.getCreatedAt(), grossAmount,
                payment.getPlatformFee(), payment.getOwnerPayout(), payment.getStatus());
    }

    // ── Lookup helpers ──────────────────────────────────────────────────

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.BOOKING_NOT_FOUND));
    }

    // ── Validation helpers ──────────────────────────────────────────────

    private void validateBookingBelongsToDriver(Booking booking, Long userId) {
        if (!booking.getDriver().getUserId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    private void validateBookingIsConfirmed(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(PaymentErrorCode.BOOKING_NOT_CONFIRMABLE_FOR_PAYMENT);
        }
    }

    private void validateStripeConfigured() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new BusinessException(PaymentErrorCode.STRIPE_CONFIG_MISSING);
        }
    }

    private void validateWebhookSecretConfigured() {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new BusinessException(PaymentErrorCode.STRIPE_CONFIG_MISSING);
        }
    }

    private void validatePaymentNotAlreadyCompleted(Payment payment) {
        if (payment.getPaymentId() != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new BusinessException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
    }

    private void validateUserHasPaymentAccess(Booking booking, Long userId) {
        boolean isDriver = booking.getDriver().getUserId().equals(userId);
        boolean isOwner = booking.getVehicle().getOwner().getUserId().equals(userId);
        if (!isDriver && !isOwner) {
            throw new BusinessException(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    // ── Stripe helpers ──────────────────────────────────────────────────

    private PaymentIntent createStripePaymentIntent(Long bookingId, BigDecimal amount) throws StripeException {
        long amountInCents = amount.multiply(CENTS_MULTIPLIER).setScale(0, RoundingMode.HALF_UP).longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("bookingId", String.valueOf(bookingId))
                .build();

        return PaymentIntent.create(params);
    }

    private PaymentIntent deserializePaymentIntent(Event event) {
        return (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new BusinessException(PaymentErrorCode.STRIPE_WEBHOOK_PROCESSING_FAILED));
    }

    private void updatePaymentStatus(String paymentIntentId, PaymentStatus status) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(payment -> {
            if (payment.getStatus() == status) {
                return;
            }
            payment.setStatus(status);
            paymentRepository.save(payment);
        });
    }

    // ── Mapping helpers ─────────────────────────────────────────────────

    private BigDecimal normalizeAmount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
