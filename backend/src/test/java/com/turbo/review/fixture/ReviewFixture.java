package com.turbo.review.fixture;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.review.model.Review;
import com.turbo.review.model.enums.ReviewType;
import com.turbo.review.service.command.CreateReviewCommand;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.vehicle.model.Vehicle;

import java.time.LocalDateTime;

public final class ReviewFixture {

    public static final Long DRIVER_USER_ID = 10L;
    public static final Long OWNER_USER_ID  = 20L;
    public static final Long OTHER_USER_ID  = 99L;
    public static final Long BOOKING_ID     = 200L;
    public static final Long VEHICLE_ID     = 100L;
    public static final Long REVIEW_ID      = 500L;

    private ReviewFixture() {}

    // ── Users ─────────────────────────────────────────────────────────

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
        owner.setFirstName("Sarah");
        owner.setLastName("Owner");
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
        return vehicle;
    }

    // ── Bookings ──────────────────────────────────────────────────────

    public static Booking completedBooking() {
        Booking booking = new Booking();
        booking.setBookingId(BOOKING_ID);
        booking.setDriver(testDriver());
        booking.setVehicle(testVehicle());
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now().minusHours(1));
        return booking;
    }

    public static Booking confirmedBooking() {
        Booking booking = completedBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCompletedAt(null);
        return booking;
    }

    // ── Reviews ───────────────────────────────────────────────────────

    public static Review driverToOwnerReview() {
        Review review = new Review();
        review.setReviewId(REVIEW_ID);
        review.setBooking(completedBooking());
        review.setReviewer(testDriver());
        review.setReviewee(testCarOwner());
        review.setVehicle(testVehicle());
        review.setReviewType(ReviewType.DRIVER_TO_OWNER);
        review.setRating(4);
        review.setComment("Great vehicle");
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }

    // ── Commands ──────────────────────────────────────────────────────

    public static CreateReviewCommand createReviewCommandAsDriver() {
        CreateReviewCommand cmd = new CreateReviewCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(DRIVER_USER_ID);
        cmd.setRating(4);
        cmd.setComment("Great vehicle");
        return cmd;
    }

    public static CreateReviewCommand createReviewCommandAsOwner() {
        CreateReviewCommand cmd = new CreateReviewCommand();
        cmd.setBookingId(BOOKING_ID);
        cmd.setUserId(OWNER_USER_ID);
        cmd.setRating(5);
        cmd.setComment("Excellent driver");
        return cmd;
    }
}
