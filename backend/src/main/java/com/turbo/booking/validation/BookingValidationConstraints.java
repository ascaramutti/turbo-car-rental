package com.turbo.booking.validation;

/**
 * Constants used for booking business rules and DTO validation constraints.
 */
public final class BookingValidationConstraints {

    private BookingValidationConstraints() {
    }

    /** Minimum allowed shift duration in hours. */
    public static final int MIN_SHIFT_HOURS = 4;

    /** Maximum allowed shift duration in hours (study permit restriction). */
    public static final int MAX_SHIFT_HOURS = 24;

    /** How many minutes before startTime a driver can start the shift early. */
    public static final int EARLY_START_MINUTES = 15;

    /** How many hours after startTime the start window expires. */
    public static final int LATE_START_HOURS = 1;

    /** Hours before startTime at which the exact location is revealed. */
    public static final int LOCATION_REVEAL_HOURS = 2;

    /** Grace period in hours after endTime before auto-completing IN_PROGRESS bookings. */
    public static final int AUTO_COMPLETE_GRACE_HOURS = 2;

    /** Maximum length for cancellation/rejection reason text. */
    public static final int MAX_REASON_LENGTH = 500;

    /** Default search radius in kilometers when none is provided. */
    public static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    /** Regex pattern for cancellation and rejection reasons. */
    public static final String REASON_PATTERN = "^[a-zA-Z0-9\u00C0-\u00FF\\s.,!?()'\\+:\\-/]*$";

    /** Regex pattern for booking status filter query parameter. */
    public static final String BOOKING_STATUS_PATTERN = "^(PENDING|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED|REJECTED)$";

    /** Masking precision factor used for ~1km coordinate rounding. */
    public static final double MASK_PRECISION_FACTOR = 100.0;

    /** Subdirectory prefix for booking photo storage. */
    public static final String BOOKING_PHOTO_DIR = "bookings";

    /** File type identifier used when storing pickup photos. */
    public static final String PICKUP_PHOTO_TYPE = "pickup";

    /** File type identifier used when storing return photos. */
    public static final String RETURN_PHOTO_TYPE = "return";

    /** Photo type label stored in BookingPhoto for pickup photos. */
    public static final String PHOTO_TYPE_PICKUP = "PICKUP";

    /** Photo type label stored in BookingPhoto for return photos. */
    public static final String PHOTO_TYPE_RETURN = "RETURN";

    /** Maximum number of photos allowed per upload. */
    public static final int MAX_PHOTOS_PER_UPLOAD = 10;

    /** Allowed photo file extensions (images only). */
    public static final java.util.Set<String> ALLOWED_PHOTO_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png");

    /** Maximum allowed photo file size in bytes (5 MB). */
    public static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;

    /** Reason stored when a booking is auto-cancelled by the system. */
    public static final String AUTO_CANCEL_REASON =
            "Auto-cancelled: owner did not respond before booking start time";

    /** Reason stored when a booking is auto-rejected after the driver's slot is confirmed on another booking. */
    public static final String AUTO_REJECT_REASON =
            "Auto-rejected: driver already has a confirmed booking for this time slot";

    /** Identifier stored in cancelledBy when the system triggers a cancellation. */
    public static final String CANCELLED_BY_SYSTEM = "SYSTEM";

    /** Identifier stored in cancelledBy when the driver cancels. */
    public static final String CANCELLED_BY_DRIVER = "DRIVER";

    /** Identifier stored in cancelledBy when the owner cancels. */
    public static final String CANCELLED_BY_OWNER = "OWNER";

    /** Cron expression: every 5 minutes for scheduled tasks. */
    public static final String EVERY_FIVE_MINUTES_CRON = "0 */5 * * * *";

    /** Warning shown when a CLASS_5 driver books a TAXI_AND_DELIVERY vehicle. */
    public static final String WARNING_CLASS5_TAXI =
            "Your Class 5 license restricts you to delivery services only";

    /** Warning shown when a CLASS_4 driver books a DELIVERY_ONLY vehicle. */
    public static final String WARNING_CLASS4_DELIVERY_ONLY =
            "This vehicle is classified for delivery services only";
}
