import api from '../../../shared/api/axios';

/**
 * Creates a Stripe PaymentIntent for a confirmed booking.
 * @param {number} bookingId
 * @returns {Promise} Axios response with { clientSecret, paymentIntentId, amount, currency, ... }
 */
export function createPaymentIntent(bookingId) {
  return api.post(`/driver/bookings/${bookingId}/payments/intent`);
}

/**
 * Confirms a payment after Stripe processes it successfully.
 * Updates the payment status from PENDING to COMPLETED in the backend.
 * @param {number} bookingId
 * @param {string} paymentIntentId - The Stripe PaymentIntent ID
 * @returns {Promise} Axios response with PaymentResponse
 */
export function confirmPayment(bookingId, paymentIntentId) {
  return api.put(`/driver/bookings/${bookingId}/payments/confirm`, null, {
    params: { paymentIntentId },
  });
}

/**
 * Retrieves the payment record for a booking.
 * @param {number} bookingId
 * @returns {Promise} Axios response with PaymentResponse
 */
export function getBookingPayment(bookingId) {
  return api.get(`/bookings/${bookingId}/payments`);
}

/**
 * Retrieves the owner's earnings summary from actual payments (80% split).
 * @returns {Promise} Axios response with OwnerEarningsResponse
 */
export function getOwnerEarnings() {
  return api.get('/owner/earnings');
}
