package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    BOOKING_NOT_FOUND("REV-001", "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_NOT_COMPLETED("REV-002", "Reviews can only be submitted for completed bookings", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_REVIEW("REV-003", "You are not authorized to review this booking", HttpStatus.FORBIDDEN),
    REVIEW_ALREADY_SUBMITTED("REV-004", "You have already submitted a review for this booking", HttpStatus.CONFLICT),
    REVIEW_NOT_FOUND("REV-005", "Review not found", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
