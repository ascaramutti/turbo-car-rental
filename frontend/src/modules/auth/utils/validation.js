/**
 * Validation patterns matching the backend API contract (Module 1 - Auth).
 * Each regex mirrors the backend @Pattern / @Size annotations.
 */
export const VALIDATION_PATTERNS = {
  email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  password: /^.{6,100}$/,
  firstName: /^[a-zA-ZÀ-ÿ' -]{1,50}$/,
  lastName: /^[a-zA-ZÀ-ÿ' -]{1,50}$/,
  phoneNumber: /^\+?[0-9\s()-]{7,20}$/,
  otp: /^\d{6}$/,
  streetAddress: /^[a-zA-Z0-9À-ÿ\s.,#\-/]+$/,
  city: /^[a-zA-ZÀ-ÿ\s.\-']+$/,
  province: /^[a-zA-ZÀ-ÿ\s.\-']+$/,
  postalCode: /^[A-Za-z]\d[A-Za-z]\s?\d[A-Za-z]\d$/,
  country: /^[a-zA-ZÀ-ÿ\s.\-']+$/,
};

/** User-facing error messages per field when pattern validation fails. */
const PATTERN_ERROR_MESSAGES = {
  email: 'Invalid email format',
  password: 'Password must be between 6 and 100 characters',
  firstName: 'Only letters, accents, apostrophes, and hyphens allowed',
  lastName: 'Only letters, accents, apostrophes, and hyphens allowed',
  phoneNumber: 'Invalid phone number format (e.g. +1 604 555-0001)',
  otp: 'OTP must be exactly 6 digits',
  streetAddress: 'Only letters, numbers, spaces, dots, commas, #, hyphens, and slashes allowed',
  city: 'Only letters, spaces, dots, hyphens, and apostrophes allowed',
  province: 'Only letters, spaces, dots, hyphens, and apostrophes allowed',
  postalCode: 'Invalid postal code format (e.g. V6B 1A1)',
  country: 'Only letters, spaces, dots, hyphens, and apostrophes allowed',
};

/** Readable field labels for "X is required" messages. */
const FIELD_LABELS = {
  email: 'Email',
  password: 'Password',
  firstName: 'First name',
  lastName: 'Last name',
  phoneNumber: 'Phone number',
  dateOfBirth: 'Date of birth',
  otp: 'OTP code',
  confirmPassword: 'Password confirmation',
  role: 'Role',
  streetAddress: 'Street address',
  city: 'City',
  province: 'Province',
  postalCode: 'Postal code',
  country: 'Country',
};

/** Fields that are optional — skip the "required" check. */
const OPTIONAL_FIELDS = new Set(['phoneNumber']);

/**
 * Validates a single field value against its API contract pattern.
 * Returns an error message string, or null if valid.
 */
export function validateField(field, value) {
  const isEmpty = !value || (typeof value === 'string' && value.trim() === '');

  if (isEmpty) {
    if (OPTIONAL_FIELDS.has(field)) return null;
    return `${FIELD_LABELS[field] || field} is required`;
  }

  const pattern = VALIDATION_PATTERNS[field];
  if (pattern && !pattern.test(value)) {
    return PATTERN_ERROR_MESSAGES[field];
  }

  return null;
}

/**
 * Validates the dateOfBirth field.
 * Must be non-empty and a date in the past.
 */
export function validateDateOfBirth(value) {
  if (!value) return 'Date of birth is required';

  const birthDate = new Date(value);
  const today = new Date();
  if (birthDate >= today) return 'Date of birth must be in the past';

  return null;
}

/**
 * Validates that password and confirmPassword match.
 */
export function validatePasswordMatch(password, confirmPassword) {
  if (!confirmPassword) return 'Please confirm your password';
  if (password !== confirmPassword) return 'Passwords do not match';
  return null;
}

/**
 * Extracts a user-friendly error message from a backend error response.
 * Handles VALIDATION-001 (field-level details) and business errors.
 */
export function extractErrorMessage(error) {
  const data = error.response?.data;
  if (!data) return 'Something went wrong. Please try again.';

  if (data.code === 'VALIDATION-001' && data.details) {
    return Object.values(data.details).join('. ');
  }

  return data.message || 'Something went wrong. Please try again.';
}
