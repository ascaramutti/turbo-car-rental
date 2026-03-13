import api from '../../../shared/api/axios';

/**
 * Registers a new user and triggers OTP email.
 * @param {Object} formData - { firstName, lastName, email, password, dateOfBirth, role, phoneNumber? }
 */
export function registerUser(formData) {
  return api.post('/auth/register', formData);
}

/**
 * Verifies the OTP code and returns JWT + user data.
 * @param {string} email
 * @param {string} otp - 6-digit code
 */
export function verifyOtp(email, otp) {
  return api.post('/auth/verify-otp', { email, otp });
}

/** Sends a new OTP code to the user's email. */
export function resendOtp(email) {
  return api.post('/auth/resend-otp', { email });
}

/**
 * Authenticates with email + password, returns JWT + user data.
 * @param {string} email
 * @param {string} password
 */
export function loginUser(email, password) {
  return api.post('/auth/login', { email, password });
}
