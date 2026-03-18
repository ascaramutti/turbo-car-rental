package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BookingErrorCode implements ErrorCode {

    DRIVER_NOT_VERIFIED("BOOK-001", "Driver must be verified before creating bookings", HttpStatus.FORBIDDEN),
    VEHICLE_NOT_APPROVED("BOOK-002", "Vehicle is not approved for rentals", HttpStatus.BAD_REQUEST),
    VEHICLE_NOT_AVAILABLE("BOOK-003", "Vehicle is not currently available for rent", HttpStatus.BAD_REQUEST),
    BOOKING_EXCEEDS_AVAILABILITY("BOOK-004", "Booking end time exceeds vehicle availability window", HttpStatus.BAD_REQUEST),
    START_TIME_NOT_FUTURE("BOOK-005", "Booking start time must be in the future", HttpStatus.BAD_REQUEST),
    END_TIME_BEFORE_START("BOOK-006", "Booking end time must be after start time", HttpStatus.BAD_REQUEST),
    MINIMUM_DURATION_NOT_MET("BOOK-007", "Minimum booking duration is 4 hours", HttpStatus.BAD_REQUEST),
    VEHICLE_ALREADY_BOOKED("BOOK-008", "Vehicle is already booked for the requested time slot", HttpStatus.CONFLICT),
    CANNOT_BOOK_OWN_VEHICLE("BOOK-009", "You cannot book your own vehicle", HttpStatus.BAD_REQUEST),
    DRIVER_HAS_OVERLAPPING_BOOKING("BOOK-010", "You already have an active booking during this time slot", HttpStatus.CONFLICT),
    BOOKING_NOT_FOUND("BOOK-011", "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_ACCESS_DENIED("BOOK-012", "You do not have permission to access this booking", HttpStatus.FORBIDDEN),
    ONLY_PENDING_CAN_BE_CONFIRMED("BOOK-013", "Only pending bookings can be confirmed", HttpStatus.BAD_REQUEST),
    ONLY_PENDING_CAN_BE_REJECTED("BOOK-014", "Only pending bookings can be rejected", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_FOR_CANCELLATION("BOOK-015", "Only pending or confirmed bookings can be cancelled", HttpStatus.BAD_REQUEST),
    ONLY_CONFIRMED_CAN_BE_STARTED("BOOK-016", "Only confirmed bookings can be started", HttpStatus.BAD_REQUEST),
    START_TOO_EARLY("BOOK-017", "Booking cannot be started yet (too early)", HttpStatus.BAD_REQUEST),
    START_WINDOW_EXPIRED("BOOK-018", "Booking start window has expired", HttpStatus.BAD_REQUEST),
    ONLY_IN_PROGRESS_CAN_BE_COMPLETED("BOOK-019", "Only in-progress bookings can be completed", HttpStatus.BAD_REQUEST),
    PICKUP_PHOTO_REQUIRED("BOOK-020", "At least one pickup photo is required to start the shift", HttpStatus.BAD_REQUEST),
    RETURN_PHOTO_REQUIRED("BOOK-021", "At least one return photo is required to complete the shift", HttpStatus.BAD_REQUEST),
    CANCELLATION_REASON_REQUIRED("BOOK-022", "Cancellation reason is required", HttpStatus.BAD_REQUEST),
    REJECTION_REASON_REQUIRED("BOOK-023", "Rejection reason is required", HttpStatus.BAD_REQUEST),
    MAXIMUM_DURATION_EXCEEDED("BOOK-025", "Maximum booking duration is 24 hours", HttpStatus.BAD_REQUEST),
    TOO_MANY_PHOTOS("BOOK-026", "Maximum 10 photos allowed per upload", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
