package com.turbo.payment.fixture;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.payment.dto.CreatePaymentIntentResponse;
import com.turbo.payment.dto.PaymentResponse;
import com.turbo.payment.model.Payment;
import com.turbo.payment.model.enums.PaymentStatus;
import com.turbo.payment.service.command.ConfirmPaymentCommand;
import com.turbo.payment.service.command.CreatePaymentIntentCommand;
import com.turbo.payment.service.command.GetPaymentCommand;
import com.turbo.payment.service.command.HandleWebhookCommand;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.vehicle.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentFixture {

    // ── Identity constants ────────────────────────────────────────────
    public static final Long DRIVER_USER_ID = 10L;
    public static final Long OWNER_USER_ID  = 20L;
    public static final Long OTHER_USER_ID  = 99L;
    public static final Long BOOKING_ID     = 200L;
    public static final Long VEHICLE_ID     = 100L;
    public static final Long PAYMENT_ID     = 300L;

    // ── Stripe constants ──────────────────────────────────────────────
    public static final String STRIPE_PI_ID      = "pi_test123abc456def";
    public static final String STRIPE_CLIENT_SECRET = "pi_test123abc456def_secret_xyz";
    public static final String STRIPE_SECRET_KEY = "sk_test_fake";
    public static final String STRIPE_WEBHOOK_SECRET = "whsec_test_fake";
    public static final String STRIPE_SIGNATURE  = "t=123,v1=abc";

    // ── Financial constants ───────────────────────────────────────────
    // Pricing model (markup): owner net rate $12.50/hr × 8h = $100 owner payout.
    // Driver-facing rate $15.00/hr × 8h = $120 total price (BASE_AMOUNT). Platform fee = $20.
    public static final BigDecimal OWNER_HOURLY_RATE = new BigDecimal("12.50");
    public static final int        TOTAL_HOURS       = 8;
    public static final BigDecimal BASE_AMOUNT       = new BigDecimal("120.0000");
    public static final BigDecimal SECURITY_DEPOSIT  = new BigDecimal("200.0000");
    public static final BigDecimal TOTAL_AMOUNT      = new BigDecimal("320.0000");
    public static final BigDecimal PLATFORM_FEE      = new BigDecimal("20.0000");
    public static final BigDecimal OWNER_PAYOUT      = new BigDecimal("100.0000");
    public static final String CURRENCY              = "cad";

    private PaymentFixture() {}

    // ── Users ─────────────────────────────────────────────────────────

    public static User driverUser() {
        User user = new User();
        user.setUserId(DRIVER_USER_ID);
        return user;
    }

    public static Driver testDriver() {
        Driver driver = new Driver();
        driver.setUserId(DRIVER_USER_ID);
        driver.setFirstName("John");
        driver.setLastName("Driver");
        return driver;
    }

    public static CarOwner testCarOwner() {
        CarOwner owner = new CarOwner();
        owner.setUserId(OWNER_USER_ID);
        return owner;
    }

    // ── Vehicles ──────────────────────────────────────────────────────

    public static Vehicle testVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(VEHICLE_ID);
        vehicle.setOwner(testCarOwner());
        vehicle.setYear(2022);
        vehicle.setMake("Toyota");
        vehicle.setModel("Camry");
        vehicle.setHourlyRate(OWNER_HOURLY_RATE);
        return vehicle;
    }

    // ── Bookings ──────────────────────────────────────────────────────

    public static Booking confirmedBooking() {
        Booking booking = new Booking();
        booking.setBookingId(BOOKING_ID);
        booking.setDriver(testDriver());
        booking.setVehicle(testVehicle());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPrice(BASE_AMOUNT);
        booking.setTotalHours(TOTAL_HOURS);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(8));
        return booking;
    }

    public static Booking pendingBooking() {
        Booking booking = confirmedBooking();
        booking.setStatus(BookingStatus.PENDING);
        return booking;
    }

    // ── Payments ──────────────────────────────────────────────────────

    public static Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setPaymentId(PAYMENT_ID);
        payment.setBooking(confirmedBooking());
        payment.setAmount(TOTAL_AMOUNT);
        payment.setPlatformFee(PLATFORM_FEE);
        payment.setOwnerPayout(OWNER_PAYOUT);
        payment.setSecurityDeposit(SECURITY_DEPOSIT);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency(CURRENCY);
        payment.setStripePaymentIntentId(STRIPE_PI_ID);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    public static Payment completedPayment() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.COMPLETED);
        return payment;
    }

    public static Payment paymentWithoutStripeId() {
        Payment payment = completedPayment();
        payment.setStripePaymentIntentId(null);
        return payment;
    }

    // ── Commands ──────────────────────────────────────────────────────

    public static CreatePaymentIntentCommand createPaymentIntentCommand() {
        CreatePaymentIntentCommand cmd = new CreatePaymentIntentCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(DRIVER_USER_ID);
        return cmd;
    }

    public static ConfirmPaymentCommand confirmPaymentCommand() {
        ConfirmPaymentCommand cmd = new ConfirmPaymentCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(DRIVER_USER_ID);
        cmd.setPaymentIntentId(STRIPE_PI_ID);
        return cmd;
    }

    public static GetPaymentCommand getPaymentCommandAsDriver() {
        GetPaymentCommand cmd = new GetPaymentCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(DRIVER_USER_ID);
        return cmd;
    }

    public static GetPaymentCommand getPaymentCommandAsOwner() {
        GetPaymentCommand cmd = new GetPaymentCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(OWNER_USER_ID);
        return cmd;
    }

    public static GetPaymentCommand getPaymentCommandAsStranger() {
        GetPaymentCommand cmd = new GetPaymentCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(OTHER_USER_ID);
        return cmd;
    }

    public static HandleWebhookCommand webhookCommand(String payload) {
        HandleWebhookCommand cmd = new HandleWebhookCommand();
        cmd.setPayload(payload);
        cmd.setStripeSignatureHeader(STRIPE_SIGNATURE);
        return cmd;
    }

    // ── Responses (for mapper mock returns) ───────────────────────────

    public static CreatePaymentIntentResponse createPaymentIntentResponse() {
        return new CreatePaymentIntentResponse(
                PAYMENT_ID, STRIPE_PI_ID, STRIPE_CLIENT_SECRET,
                PaymentStatus.PENDING, TOTAL_AMOUNT, PLATFORM_FEE,
                OWNER_PAYOUT, SECURITY_DEPOSIT, CURRENCY);
    }

    public static PaymentResponse paymentResponse(PaymentStatus status) {
        return new PaymentResponse(
                PAYMENT_ID, BOOKING_ID, status, TOTAL_AMOUNT,
                PLATFORM_FEE, OWNER_PAYOUT, SECURITY_DEPOSIT, CURRENCY,
                STRIPE_PI_ID, LocalDateTime.now(), LocalDateTime.now());
    }
}
