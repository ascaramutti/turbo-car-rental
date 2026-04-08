import api from '../../../shared/api/axios';

/**
 * Creates a review for a completed booking.
 * @param {number} bookingId
 * @param {{ rating: number, comment?: string }} data
 * @returns {Promise} Axios response with ReviewResponse
 */
export function createReview(bookingId, data) {
  return api.post(`/bookings/${bookingId}/reviews`, data);
}

/**
 * Gets all reviews for a booking.
 * @param {number} bookingId
 * @returns {Promise} Axios response with ReviewResponse[]
 */
export function getBookingReviews(bookingId) {
  return api.get(`/bookings/${bookingId}/reviews`);
}

/**
 * Gets all reviews received by a user.
 * @param {number} userId
 * @returns {Promise} Axios response with ReviewResponse[]
 */
export function getUserReviews(userId) {
  return api.get(`/users/${userId}/reviews`);
}
