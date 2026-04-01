package com.turbo.payment.service.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
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
import com.turbo.payment.dto.OwnerEarningsResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.fixture.PaymentFixture;
import com.turbo.payment.model.Payment;
import com.turbo.payment.model.enums.PaymentStatus;
import com.turbo.payment.repository.PaymentRepository;
import com.turbo.payment.service.command.ConfirmPaymentCommand;
import com.turbo.payment.service.command.CreatePaymentIntentCommand;
import com.turbo.payment.service.command.GetPaymentCommand;
import com.turbo.payment.service.command.HandleWebhookCommand;
import com.turbo.payment.service.mapper.PaymentServiceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentServiceMapper serviceMapper;

    @InjectMocks private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", PaymentFixture.STRIPE_SECRET_KEY);
        ReflectionTestUtils.setField(paymentService, "stripeWebhookSecret", PaymentFixture.STRIPE_WEBHOOK_SECRET);
        ReflectionTestUtils.setField(paymentService, "currency", PaymentFixture.CURRENCY);
        ReflectionTestUtils.setField(paymentService, "securityDeposit", PaymentFixture.SECURITY_DEPOSIT);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private void mockBookingFound(Booking booking) {
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
    }

    private void mockBookingNotFound() {
        when(bookingRepository.findById(PaymentFixture.BOOKING_ID)).thenReturn(Optional.empty());
    }

    private void mockPaymentFoundByBookingId(Payment payment) {
        when(paymentRepository.findByBookingBookingId(PaymentFixture.BOOKING_ID))
                .thenReturn(Optional.of(payment));
    }

    private void mockNoPaymentForBooking() {
        when(paymentRepository.findByBookingBookingId(PaymentFixture.BOOKING_ID))
                .thenReturn(Optional.empty());
    }

    private void mockPaymentSaved() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(PaymentFixture.PAYMENT_ID);
            }
            return p;
        });
    }

    // ── createPaymentIntent ───────────────────────────────────────────

    @Nested
    @DisplayName("createPaymentIntent()")
    class CreatePaymentIntent {

        @Test
        @DisplayName("Happy path — creates PENDING payment with 80/20 split")
        void createPaymentIntent_happyPath_returnsPendingPayment() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            mockNoPaymentForBooking();
            mockPaymentSaved();

            CreatePaymentIntentResponse expectedResponse = PaymentFixture.createPaymentIntentResponse();
            when(serviceMapper.toCreatePaymentIntentResponse(any(Payment.class), anyString(), anyString()))
                    .thenReturn(expectedResponse);

            try (MockedStatic<PaymentIntent> piStatic = mockStatic(PaymentIntent.class)) {
                PaymentIntent mockIntent = mock(PaymentIntent.class);
                when(mockIntent.getId()).thenReturn(PaymentFixture.STRIPE_PI_ID);
                when(mockIntent.getClientSecret()).thenReturn(PaymentFixture.STRIPE_CLIENT_SECRET);
                piStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenReturn(mockIntent);

                CreatePaymentIntentResponse result = paymentService.createPaymentIntent(
                        PaymentFixture.createPaymentIntentCommand());

                assertThat(result).isNotNull();
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
                verify(paymentRepository).save(any(Payment.class));
            }
        }

        @Test
        @DisplayName("Idempotent — reuses existing PENDING payment")
        void createPaymentIntent_existingPending_reusesPayment() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment existing = PaymentFixture.pendingPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(existing);
            mockPaymentSaved();

            CreatePaymentIntentResponse expectedResponse = PaymentFixture.createPaymentIntentResponse();
            when(serviceMapper.toCreatePaymentIntentResponse(any(Payment.class), anyString(), anyString()))
                    .thenReturn(expectedResponse);

            try (MockedStatic<PaymentIntent> piStatic = mockStatic(PaymentIntent.class)) {
                PaymentIntent mockIntent = mock(PaymentIntent.class);
                when(mockIntent.getId()).thenReturn(PaymentFixture.STRIPE_PI_ID);
                when(mockIntent.getClientSecret()).thenReturn(PaymentFixture.STRIPE_CLIENT_SECRET);
                piStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenReturn(mockIntent);

                CreatePaymentIntentResponse result = paymentService.createPaymentIntent(
                        PaymentFixture.createPaymentIntentCommand());

                assertThat(result).isNotNull();
                verify(paymentRepository).save(any(Payment.class));
            }
        }

        @Test
        @DisplayName("Verifies financial calculations: platformFee=20%, ownerPayout=80%")
        void createPaymentIntent_verifiesFinancialCalculations() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            mockNoPaymentForBooking();
            mockPaymentSaved();

            when(serviceMapper.toCreatePaymentIntentResponse(any(Payment.class), anyString(), anyString()))
                    .thenReturn(PaymentFixture.createPaymentIntentResponse());

            try (MockedStatic<PaymentIntent> piStatic = mockStatic(PaymentIntent.class)) {
                PaymentIntent mockIntent = mock(PaymentIntent.class);
                when(mockIntent.getId()).thenReturn(PaymentFixture.STRIPE_PI_ID);
                when(mockIntent.getClientSecret()).thenReturn(PaymentFixture.STRIPE_CLIENT_SECRET);
                piStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenReturn(mockIntent);

                paymentService.createPaymentIntent(PaymentFixture.createPaymentIntentCommand());

                verify(paymentRepository).save(argThat(payment -> {
                    assertThat(payment.getPlatformFee()).isEqualByComparingTo("20.0000");
                    assertThat(payment.getOwnerPayout()).isEqualByComparingTo("80.0000");
                    assertThat(payment.getSecurityDeposit()).isEqualByComparingTo("200.0000");
                    assertThat(payment.getAmount()).isEqualByComparingTo("300.0000");
                    assertThat(payment.getCurrency()).isEqualTo("cad");
                    return true;
                }));
            }
        }

        @Test
        @DisplayName("PAY-001: booking not found — throws BOOKING_NOT_FOUND")
        void createPaymentIntent_bookingNotFound_throwsPay001() {
            mockBookingNotFound();

            assertThatThrownBy(() -> paymentService.createPaymentIntent(PaymentFixture.createPaymentIntentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("PAY-002: booking belongs to another driver — throws UNAUTHORIZED_PAYMENT_ACCESS")
        void createPaymentIntent_wrongDriver_throwsPay002() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);

            CreatePaymentIntentCommand cmd = PaymentFixture.createPaymentIntentCommand();
            cmd.setUserId(PaymentFixture.OTHER_USER_ID);

            assertThatThrownBy(() -> paymentService.createPaymentIntent(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        @Test
        @DisplayName("PAY-003: booking not CONFIRMED — throws BOOKING_NOT_CONFIRMABLE_FOR_PAYMENT")
        void createPaymentIntent_bookingNotConfirmed_throwsPay003() {
            Booking booking = PaymentFixture.pendingBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> paymentService.createPaymentIntent(PaymentFixture.createPaymentIntentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.BOOKING_NOT_CONFIRMABLE_FOR_PAYMENT);
        }

        @Test
        @DisplayName("PAY-004: Stripe not configured — throws STRIPE_CONFIG_MISSING")
        void createPaymentIntent_stripeNotConfigured_throwsPay004() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "");

            assertThatThrownBy(() -> paymentService.createPaymentIntent(PaymentFixture.createPaymentIntentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.STRIPE_CONFIG_MISSING);
        }

        @Test
        @DisplayName("PAY-005: payment already COMPLETED — throws PAYMENT_ALREADY_COMPLETED")
        void createPaymentIntent_alreadyCompleted_throwsPay005() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment completed = PaymentFixture.completedPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(completed);

            assertThatThrownBy(() -> paymentService.createPaymentIntent(PaymentFixture.createPaymentIntentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        @Test
        @DisplayName("PAY-006: Stripe API fails — throws STRIPE_PAYMENT_INTENT_FAILED")
        void createPaymentIntent_stripeFails_throwsPay006() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            mockNoPaymentForBooking();

            try (MockedStatic<PaymentIntent> piStatic = mockStatic(PaymentIntent.class)) {
                piStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenThrow(mock(StripeException.class));

                assertThatThrownBy(() -> paymentService.createPaymentIntent(
                        PaymentFixture.createPaymentIntentCommand()))
                        .isInstanceOf(BusinessException.class)
                        .extracting(ex -> ((BusinessException) ex).getErrorCode())
                        .isEqualTo(PaymentErrorCode.STRIPE_PAYMENT_INTENT_FAILED);
            }
        }
    }

    // ── confirmPayment ────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPayment {

        @Test
        @DisplayName("Happy path — transitions PENDING to COMPLETED")
        void confirmPayment_happyPath_returnsCompleted() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.pendingPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(payment);
            mockPaymentSaved();

            PaymentResponse expectedResponse = PaymentFixture.paymentResponse(PaymentStatus.COMPLETED);
            when(serviceMapper.toPaymentResponse(any(Payment.class))).thenReturn(expectedResponse);

            PaymentResponse result = paymentService.confirmPayment(PaymentFixture.confirmPaymentCommand());

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.COMPLETED));
        }

        @Test
        @DisplayName("Idempotent — already COMPLETED returns existing without save")
        void confirmPayment_alreadyCompleted_returnsExisting() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment completed = PaymentFixture.completedPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(completed);

            PaymentResponse expectedResponse = PaymentFixture.paymentResponse(PaymentStatus.COMPLETED);
            when(serviceMapper.toPaymentResponse(completed)).thenReturn(expectedResponse);

            PaymentResponse result = paymentService.confirmPayment(PaymentFixture.confirmPaymentCommand());

            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("PAY-001: booking not found — throws BOOKING_NOT_FOUND")
        void confirmPayment_bookingNotFound_throwsPay001() {
            mockBookingNotFound();

            assertThatThrownBy(() -> paymentService.confirmPayment(PaymentFixture.confirmPaymentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("PAY-002: wrong driver — throws UNAUTHORIZED_PAYMENT_ACCESS")
        void confirmPayment_wrongDriver_throwsPay002() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);

            ConfirmPaymentCommand cmd = PaymentFixture.confirmPaymentCommand();
            cmd.setUserId(PaymentFixture.OTHER_USER_ID);

            assertThatThrownBy(() -> paymentService.confirmPayment(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        @Test
        @DisplayName("PAY-007: payment not found — throws PAYMENT_NOT_FOUND")
        void confirmPayment_noPayment_throwsPay007() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            mockNoPaymentForBooking();

            assertThatThrownBy(() -> paymentService.confirmPayment(PaymentFixture.confirmPaymentCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("PAY-002: paymentIntentId mismatch — throws UNAUTHORIZED_PAYMENT_ACCESS")
        void confirmPayment_intentIdMismatch_throwsPay002() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.pendingPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(payment);

            ConfirmPaymentCommand cmd = PaymentFixture.confirmPaymentCommand();
            cmd.setPaymentIntentId("pi_wrongIntentId");

            assertThatThrownBy(() -> paymentService.confirmPayment(cmd))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    // ── getBookingPayment ─────────────────────────────────────────────

    @Nested
    @DisplayName("getBookingPayment()")
    class GetBookingPayment {

        @Test
        @DisplayName("Happy path — driver can view payment")
        void getBookingPayment_asDriver_returnsPayment() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.completedPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(payment);

            PaymentResponse expectedResponse = PaymentFixture.paymentResponse(PaymentStatus.COMPLETED);
            when(serviceMapper.toPaymentResponse(payment)).thenReturn(expectedResponse);

            PaymentResponse result = paymentService.getBookingPayment(PaymentFixture.getPaymentCommandAsDriver());

            assertThat(result).isNotNull();
            assertThat(result.getPaymentId()).isEqualTo(PaymentFixture.PAYMENT_ID);
        }

        @Test
        @DisplayName("Happy path — vehicle owner can view payment")
        void getBookingPayment_asOwner_returnsPayment() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.completedPayment();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(payment);

            PaymentResponse expectedResponse = PaymentFixture.paymentResponse(PaymentStatus.COMPLETED);
            when(serviceMapper.toPaymentResponse(payment)).thenReturn(expectedResponse);

            PaymentResponse result = paymentService.getBookingPayment(PaymentFixture.getPaymentCommandAsOwner());

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("PAY-001: booking not found — throws BOOKING_NOT_FOUND")
        void getBookingPayment_bookingNotFound_throwsPay001() {
            mockBookingNotFound();

            assertThatThrownBy(() -> paymentService.getBookingPayment(PaymentFixture.getPaymentCommandAsDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("PAY-002: neither driver nor owner — throws UNAUTHORIZED_PAYMENT_ACCESS")
        void getBookingPayment_stranger_throwsPay002() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> paymentService.getBookingPayment(PaymentFixture.getPaymentCommandAsStranger()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }

        @Test
        @DisplayName("PAY-007: payment not found — throws PAYMENT_NOT_FOUND")
        void getBookingPayment_noPayment_throwsPay007() {
            Booking booking = PaymentFixture.confirmedBooking();
            mockBookingFound(booking);
            mockPaymentFoundByBookingId(PaymentFixture.completedPayment());

            // Override mock for this case
            when(paymentRepository.findByBookingBookingId(PaymentFixture.BOOKING_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getBookingPayment(PaymentFixture.getPaymentCommandAsDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    // ── handleWebhookEvent ────────────────────────────────────────────

    @Nested
    @DisplayName("handleWebhookEvent()")
    class HandleWebhookEvent {

        @Test
        @DisplayName("payment_intent.succeeded — sets status to COMPLETED")
        void handleWebhook_succeeded_setsCompleted() {
            Payment payment = PaymentFixture.pendingPayment();
            when(paymentRepository.findByStripePaymentIntentId(PaymentFixture.STRIPE_PI_ID))
                    .thenReturn(Optional.of(payment));
            mockPaymentSaved();

            try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
                Event event = mockStripeEvent("payment_intent.succeeded");
                webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                        .thenReturn(event);

                paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}"));

                verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.COMPLETED));
            }
        }

        @Test
        @DisplayName("payment_intent.payment_failed — sets status to FAILED")
        void handleWebhook_failed_setsFailed() {
            Payment payment = PaymentFixture.pendingPayment();
            when(paymentRepository.findByStripePaymentIntentId(PaymentFixture.STRIPE_PI_ID))
                    .thenReturn(Optional.of(payment));
            mockPaymentSaved();

            try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
                Event event = mockStripeEvent("payment_intent.payment_failed");
                webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                        .thenReturn(event);

                paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}"));

                verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
            }
        }

        @Test
        @DisplayName("Unsupported event type — ignored, no save")
        void handleWebhook_unsupportedType_ignored() {
            try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
                Event event = mock(Event.class);
                when(event.getType()).thenReturn("charge.refunded");
                webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                        .thenReturn(event);

                paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}"));

                verify(paymentRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("PAY-004: webhook secret not configured — throws STRIPE_CONFIG_MISSING")
        void handleWebhook_noSecret_throwsPay004() {
            ReflectionTestUtils.setField(paymentService, "stripeWebhookSecret", "");

            assertThatThrownBy(() -> paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(PaymentErrorCode.STRIPE_CONFIG_MISSING);
        }

        @Test
        @DisplayName("PAY-008: invalid signature — throws INVALID_STRIPE_WEBHOOK_SIGNATURE")
        void handleWebhook_invalidSignature_throwsPay008() {
            try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
                webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                        .thenThrow(new SignatureVerificationException("bad sig", null));

                assertThatThrownBy(() -> paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}")))
                        .isInstanceOf(BusinessException.class)
                        .extracting(ex -> ((BusinessException) ex).getErrorCode())
                        .isEqualTo(PaymentErrorCode.INVALID_STRIPE_WEBHOOK_SIGNATURE);
            }
        }

        @Test
        @DisplayName("PAY-009: deserialization fails — throws STRIPE_WEBHOOK_PROCESSING_FAILED")
        void handleWebhook_deserializationError_throwsPay009() {
            try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
                Event event = mock(Event.class);
                when(event.getType()).thenReturn("payment_intent.succeeded");
                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(event.getDataObjectDeserializer()).thenReturn(deserializer);
                when(deserializer.getObject()).thenReturn(Optional.empty());

                webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                        .thenReturn(event);

                assertThatThrownBy(() -> paymentService.handleWebhookEvent(PaymentFixture.webhookCommand("{}")))
                        .isInstanceOf(BusinessException.class)
                        .extracting(ex -> ((BusinessException) ex).getErrorCode())
                        .isEqualTo(PaymentErrorCode.STRIPE_WEBHOOK_PROCESSING_FAILED);
            }
        }

        private Event mockStripeEvent(String eventType) {
            Event event = mock(Event.class);
            when(event.getType()).thenReturn(eventType);
            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn(PaymentFixture.STRIPE_PI_ID);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));
            return event;
        }
    }

    // ── refundPaymentForCancelledBooking ───────────────────────────────

    @Nested
    @DisplayName("refundPaymentForCancelledBooking()")
    class RefundPayment {

        @Test
        @DisplayName("Happy path — COMPLETED payment gets refunded via Stripe")
        void refund_completedPayment_refundsAndSetsStatus() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.completedPayment();
            when(paymentRepository.findByBookingBookingId(booking.getBookingId()))
                    .thenReturn(Optional.of(payment));
            mockPaymentSaved();

            try (MockedStatic<Refund> refundStatic = mockStatic(Refund.class)) {
                refundStatic.when(() -> Refund.create(any(RefundCreateParams.class)))
                        .thenReturn(mock(Refund.class));

                paymentService.refundPaymentForCancelledBooking(booking);

                verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
            }
        }

        @Test
        @DisplayName("No payment exists — no-op, no save")
        void refund_noPayment_noOp() {
            Booking booking = PaymentFixture.confirmedBooking();
            when(paymentRepository.findByBookingBookingId(booking.getBookingId()))
                    .thenReturn(Optional.empty());

            paymentService.refundPaymentForCancelledBooking(booking);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Payment is PENDING (not COMPLETED) — no-op, no refund")
        void refund_pendingPayment_noOp() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.pendingPayment();
            when(paymentRepository.findByBookingBookingId(booking.getBookingId()))
                    .thenReturn(Optional.of(payment));

            paymentService.refundPaymentForCancelledBooking(booking);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("No stripePaymentIntentId — no-op, no refund")
        void refund_noStripeId_noOp() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.paymentWithoutStripeId();
            when(paymentRepository.findByBookingBookingId(booking.getBookingId()))
                    .thenReturn(Optional.of(payment));

            paymentService.refundPaymentForCancelledBooking(booking);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Stripe refund fails — logs warning but still marks REFUNDED")
        void refund_stripeFails_stillMarksRefunded() {
            Booking booking = PaymentFixture.confirmedBooking();
            Payment payment = PaymentFixture.completedPayment();
            when(paymentRepository.findByBookingBookingId(booking.getBookingId()))
                    .thenReturn(Optional.of(payment));
            mockPaymentSaved();

            try (MockedStatic<Refund> refundStatic = mockStatic(Refund.class)) {
                refundStatic.when(() -> Refund.create(any(RefundCreateParams.class)))
                        .thenThrow(mock(StripeException.class));

                paymentService.refundPaymentForCancelledBooking(booking);

                verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
            }
        }
    }

    // ── getOwnerEarnings ──────────────────────────────────────────────

    @Nested
    @DisplayName("getOwnerEarnings()")
    class GetOwnerEarnings {

        @Test
        @DisplayName("Happy path — returns earnings summary with transactions")
        void getOwnerEarnings_happyPath_returnsSummary() {
            Payment payment = PaymentFixture.completedPayment();
            when(paymentRepository.sumOwnerPayoutByStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.COMPLETED))
                    .thenReturn(PaymentFixture.OWNER_PAYOUT);
            when(paymentRepository.sumOwnerPayoutByStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.PENDING))
                    .thenReturn(BigDecimal.ZERO);
            when(paymentRepository.sumOwnerPayoutInRange(eq(PaymentFixture.OWNER_USER_ID), any(), any()))
                    .thenReturn(PaymentFixture.OWNER_PAYOUT);
            when(paymentRepository.countByOwnerAndStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.COMPLETED))
                    .thenReturn(1L);
            when(paymentRepository.findByOwnerOrderByCreatedAtDesc(PaymentFixture.OWNER_USER_ID))
                    .thenReturn(List.of(payment));

            OwnerEarningsResponse result = paymentService.getOwnerEarnings(PaymentFixture.OWNER_USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getTotalEarnings()).isEqualByComparingTo("80.0000");
            assertThat(result.getMonthEarnings()).isEqualByComparingTo("80.0000");
            assertThat(result.getPendingPayouts()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getCompletedPayments()).isEqualTo(1);
            assertThat(result.getTransactions()).hasSize(1);
            assertThat(result.getTransactions().get(0).getOwnerPayout())
                    .isEqualByComparingTo("80.0000");
        }

        @Test
        @DisplayName("No payments — returns zeros with empty transactions")
        void getOwnerEarnings_noPayments_returnsZeros() {
            when(paymentRepository.sumOwnerPayoutByStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.COMPLETED))
                    .thenReturn(BigDecimal.ZERO);
            when(paymentRepository.sumOwnerPayoutByStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.PENDING))
                    .thenReturn(BigDecimal.ZERO);
            when(paymentRepository.sumOwnerPayoutInRange(eq(PaymentFixture.OWNER_USER_ID), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(paymentRepository.countByOwnerAndStatus(PaymentFixture.OWNER_USER_ID, PaymentStatus.COMPLETED))
                    .thenReturn(0L);
            when(paymentRepository.findByOwnerOrderByCreatedAtDesc(PaymentFixture.OWNER_USER_ID))
                    .thenReturn(List.of());

            OwnerEarningsResponse result = paymentService.getOwnerEarnings(PaymentFixture.OWNER_USER_ID);

            assertThat(result.getTotalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getCompletedPayments()).isZero();
            assertThat(result.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("Transaction maps vehicle summary and driver name correctly")
        void getOwnerEarnings_transactionMapping_correct() {
            Payment payment = PaymentFixture.completedPayment();
            when(paymentRepository.sumOwnerPayoutByStatus(any(), any())).thenReturn(BigDecimal.ZERO);
            when(paymentRepository.sumOwnerPayoutInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(paymentRepository.countByOwnerAndStatus(any(), any())).thenReturn(0L);
            when(paymentRepository.findByOwnerOrderByCreatedAtDesc(PaymentFixture.OWNER_USER_ID))
                    .thenReturn(List.of(payment));

            OwnerEarningsResponse result = paymentService.getOwnerEarnings(PaymentFixture.OWNER_USER_ID);

            assertThat(result.getTransactions()).hasSize(1);
            var tx = result.getTransactions().get(0);
            assertThat(tx.getBookingId()).isEqualTo(PaymentFixture.BOOKING_ID);
            assertThat(tx.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(tx.getGrossAmount()).isEqualByComparingTo("100.0000");
            assertThat(tx.getPlatformFee()).isEqualByComparingTo("20.0000");
        }
    }
}
