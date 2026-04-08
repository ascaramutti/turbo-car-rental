/** Review type enum values matching ReviewType on the backend. */
export const REVIEW_TYPE = {
  DRIVER_TO_OWNER: 'DRIVER_TO_OWNER',
  OWNER_TO_DRIVER: 'OWNER_TO_DRIVER',
};

/** Review type display labels. */
export const REVIEW_TYPE_LABELS = {
  [REVIEW_TYPE.DRIVER_TO_OWNER]: 'Driver → Owner',
  [REVIEW_TYPE.OWNER_TO_DRIVER]: 'Owner → Driver',
};

/** Validation constraints matching backend. */
export const REVIEW_CONSTRAINTS = {
  MIN_RATING: 1,
  MAX_RATING: 5,
  MAX_COMMENT_LENGTH: 500,
  COMMENT_PATTERN: /^[a-zA-Z0-9 .,!?'\-()]*$/,
};

/** User-facing error messages for REV-XXX error codes. */
export const REVIEW_ERROR_MESSAGES = {
  'REV-001': 'Booking not found',
  'REV-002': 'Reviews can only be submitted for completed bookings',
  'REV-003': 'You are not authorized to review this booking',
  'REV-004': 'You have already submitted a review for this booking',
  'REV-005': 'Review not found',
};
