package com.turbo.booking.validation;

/**
 * User-facing validation messages for booking request DTO annotations.
 */
public final class BookingValidationMessages {

    private BookingValidationMessages() {
    }

    public static final String VEHICLE_ID_REQUIRED = "Vehicle ID is required";
    public static final String START_TIME_REQUIRED = "Start time is required";
    public static final String START_TIME_FUTURE = "Start time must be in the future";
    public static final String END_TIME_REQUIRED = "End time is required";
    public static final String END_TIME_FUTURE = "End time must be in the future";
    public static final String REASON_REQUIRED = "Reason is required";
    public static final String REASON_SIZE = "Reason cannot exceed 500 characters";
    public static final String REASON_PATTERN_MSG =
            "Reason can only contain letters, numbers, spaces, and basic punctuation";
    public static final String VEHICLE_ID_INVALID = "Vehicle ID must be a positive number";
}
