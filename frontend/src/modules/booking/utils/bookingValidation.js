import { MIN_SHIFT_HOURS, MAX_SHIFT_HOURS, MAX_REASON_LENGTH } from '../constants/bookingConstants';

/**
 * Regex matching backend BookingValidationConstraints.REASON_PATTERN exactly.
 * Allows letters (including À-ÿ accented), numbers, spaces, and basic punctuation.
 */
const REASON_PATTERN = /^[a-zA-Z0-9\u00C0-\u00FF\s.,!?()'\\+:\-/]*$/;

/**
 * Validates the startTime field.
 * Must be present and in the future.
 * @param {string} startTime - ISO datetime string
 * @returns {string|null} Error message or null
 */
export function validateStartTime(startTime) {
  if (!startTime) return 'Start time is required';
  if (new Date(startTime) <= new Date()) return 'Start time must be in the future';
  return null;
}

/**
 * Validates the endTime field relative to startTime.
 * Must be present, after startTime, and within MIN_SHIFT_HOURS to MAX_SHIFT_HOURS.
 * @param {string} endTime - ISO datetime string
 * @param {string} startTime - ISO datetime string
 * @returns {string|null} Error message or null
 */
export function validateEndTime(endTime, startTime) {
  if (!endTime) return 'End time is required';
  if (!startTime) return null;

  const start = new Date(startTime);
  const end = new Date(endTime);

  if (end <= start) return 'End time must be after start time';

  const diffHours = (end - start) / (1000 * 60 * 60);

  if (diffHours < MIN_SHIFT_HOURS) {
    return `Minimum booking duration is ${MIN_SHIFT_HOURS} hours`;
  }
  if (diffHours > MAX_SHIFT_HOURS) {
    return `Maximum booking duration is ${MAX_SHIFT_HOURS} hours`;
  }

  return null;
}

/**
 * Validates a cancellation or rejection reason.
 * Must be present, not exceed MAX_REASON_LENGTH, and match the backend pattern.
 * @param {string} reason - The reason text
 * @returns {string|null} Error message or null
 */
export function validateReason(reason) {
  if (!reason || !reason.trim()) return 'Reason is required';
  if (reason.trim().length > MAX_REASON_LENGTH) {
    return `Reason cannot exceed ${MAX_REASON_LENGTH} characters`;
  }
  if (!REASON_PATTERN.test(reason)) {
    return 'Reason can only contain letters, numbers, spaces, and basic punctuation';
  }
  return null;
}

/**
 * Validates a single booking form field by name.
 * @param {string} fieldName - The field identifier
 * @param {*} value - The current field value
 * @param {Object} formData - The full form data object (required for cross-field validation)
 * @returns {string|null} Error message or null
 */
export function validateBookingField(fieldName, value, formData = {}) {
  switch (fieldName) {
    case 'startTime':
      return validateStartTime(value);
    case 'endTime':
      return validateEndTime(value, formData.startTime);
    case 'reason':
      return validateReason(value);
    case 'vehicleId':
      return !value ? 'Vehicle ID is required' : null;
    default:
      return null;
  }
}

/**
 * Validates all fields in the booking creation form.
 * @param {Object} data - Form data containing vehicleId, startTime, endTime
 * @returns {Object} Map of fieldName -> error message (empty if valid)
 */
export function validateCreateBookingForm(data) {
  const errors = {};

  const vehicleError = validateBookingField('vehicleId', data.vehicleId);
  if (vehicleError) errors.vehicleId = vehicleError;

  const startError = validateStartTime(data.startTime);
  if (startError) errors.startTime = startError;

  const endError = validateEndTime(data.endTime, data.startTime);
  if (endError) errors.endTime = endError;

  return errors;
}
