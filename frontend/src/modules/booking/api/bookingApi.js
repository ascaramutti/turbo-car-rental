import api from '../../../shared/api/axios';
import { BOOKING_ENDPOINTS } from '../constants/bookingConstants';

/** Headers required for multipart file uploads. */
const MULTIPART_HEADERS = { 'Content-Type': 'multipart/form-data' };

// ── Driver — Vehicle Search ──────────────────────────────────────────────

/**
 * Searches available vehicles with optional filters.
 * Returns masked coordinates and service type warnings per vehicle.
 * @param {Object} params - Query parameters (latitude, longitude, radiusKm, startTime, endTime, category, serviceType, minPrice, maxPrice, fuelType)
 * @returns {Promise} Axios response with List<VehicleSearchResponse>
 */
export function searchVehicles(params = {}) {
  return api.get(BOOKING_ENDPOINTS.SEARCH_VEHICLES, { params });
}

/**
 * Returns full detail for a specific vehicle (still with masked coordinates).
 * @param {number} vehicleId - Vehicle ID
 * @returns {Promise} Axios response with VehicleBookingDetailResponse
 */
export function getVehicleDetail(vehicleId) {
  return api.get(BOOKING_ENDPOINTS.VEHICLE_DETAIL(vehicleId));
}

// ── Driver — Booking Lifecycle ───────────────────────────────────────────

/**
 * Creates a new booking for the authenticated driver.
 * @param {Object} data - { vehicleId, startTime, endTime }
 * @returns {Promise} Axios response with BookingResponse
 */
export function createBooking(data) {
  return api.post(BOOKING_ENDPOINTS.CREATE_BOOKING, data);
}

/**
 * Returns the authenticated driver's bookings, optionally filtered by status.
 * @param {string|null} status - Optional booking status filter
 * @returns {Promise} Axios response with List<BookingResponse>
 */
export function getDriverBookings(status = null) {
  const params = status ? { status } : {};
  return api.get(BOOKING_ENDPOINTS.GET_DRIVER_BOOKINGS, { params });
}

/**
 * Returns the full detail of one of the driver's bookings.
 * @param {number} bookingId - Booking ID
 * @returns {Promise} Axios response with BookingDetailResponse
 */
export function getDriverBookingDetail(bookingId) {
  return api.get(BOOKING_ENDPOINTS.GET_DRIVER_BOOKING_DETAIL(bookingId));
}

/**
 * Cancels a PENDING or CONFIRMED booking on behalf of the driver.
 * @param {number} bookingId - Booking ID
 * @param {string} reason - Cancellation reason (required, max 500 chars)
 * @returns {Promise} Axios response with BookingResponse
 */
export function cancelDriverBooking(bookingId, reason) {
  return api.put(BOOKING_ENDPOINTS.CANCEL_DRIVER_BOOKING(bookingId), { reason });
}

/**
 * Starts a CONFIRMED booking (shift pickup) by uploading one or more pickup photos.
 * @param {number} bookingId - Booking ID
 * @param {File[]} photos - Pickup condition photos (JPG/PNG, max 5MB each, 1–10 files)
 * @returns {Promise} Axios response with BookingResponse
 */
export function startBooking(bookingId, photos) {
  const formData = new FormData();
  photos.forEach((photo) => formData.append('pickupPhotos', photo));
  return api.put(BOOKING_ENDPOINTS.START_BOOKING(bookingId), formData, {
    headers: MULTIPART_HEADERS,
  });
}

/**
 * Completes an IN_PROGRESS booking (shift return) by uploading one or more return photos.
 * @param {number} bookingId - Booking ID
 * @param {File[]} photos - Return condition photos (JPG/PNG, max 5MB each, 1–10 files)
 * @returns {Promise} Axios response with BookingResponse
 */
export function completeBooking(bookingId, photos) {
  const formData = new FormData();
  photos.forEach((photo) => formData.append('returnPhotos', photo));
  return api.put(BOOKING_ENDPOINTS.COMPLETE_BOOKING(bookingId), formData, {
    headers: MULTIPART_HEADERS,
  });
}

/**
 * Returns the vehicle location for a confirmed booking.
 * Location may be masked (>2h before start) or exact (<=2h before start).
 * @param {number} bookingId - Booking ID
 * @returns {Promise} Axios response with VehicleLocationResponse
 */
export function getVehicleLocation(bookingId) {
  return api.get(BOOKING_ENDPOINTS.GET_VEHICLE_LOCATION(bookingId));
}

// ── Owner — Booking Management ───────────────────────────────────────────

/**
 * Returns bookings for all vehicles owned by the authenticated car owner.
 * @param {string|null} status - Optional booking status filter
 * @param {number|null} vehicleId - Optional vehicle ID filter
 * @returns {Promise} Axios response with List<BookingResponse>
 */
export function getOwnerBookings(status = null, vehicleId = null) {
  const params = {};
  if (status) params.status = status;
  if (vehicleId) params.vehicleId = vehicleId;
  return api.get(BOOKING_ENDPOINTS.GET_OWNER_BOOKINGS, { params });
}

/**
 * Returns full detail of a booking for one of the owner's vehicles.
 * @param {number} bookingId - Booking ID
 * @returns {Promise} Axios response with BookingDetailResponse
 */
export function getOwnerBookingDetail(bookingId) {
  return api.get(BOOKING_ENDPOINTS.GET_OWNER_BOOKING_DETAIL(bookingId));
}

/**
 * Confirms a PENDING booking (no request body required).
 * @param {number} bookingId - Booking ID
 * @returns {Promise} Axios response with BookingResponse
 */
export function confirmBooking(bookingId) {
  return api.put(BOOKING_ENDPOINTS.CONFIRM_BOOKING(bookingId));
}

/**
 * Rejects a PENDING booking with a mandatory reason.
 * @param {number} bookingId - Booking ID
 * @param {string} reason - Rejection reason (required, max 500 chars)
 * @returns {Promise} Axios response with BookingResponse
 */
export function rejectBooking(bookingId, reason) {
  return api.put(BOOKING_ENDPOINTS.REJECT_BOOKING(bookingId), { reason });
}

/**
 * Cancels a CONFIRMED booking on behalf of the car owner.
 * @param {number} bookingId - Booking ID
 * @param {string} reason - Cancellation reason (required, max 500 chars)
 * @returns {Promise} Axios response with BookingResponse
 */
export function cancelOwnerBooking(bookingId, reason) {
  return api.put(BOOKING_ENDPOINTS.CANCEL_OWNER_BOOKING(bookingId), { reason });
}

// ── Admin — Read-Only Overview ───────────────────────────────────────────

/**
 * Returns all bookings on the platform (admin only).
 * @param {string|null} status - Optional status filter
 * @param {number|null} driverId - Optional driver ID filter
 * @param {number|null} vehicleId - Optional vehicle ID filter
 * @returns {Promise} Axios response with List<AdminBookingResponse>
 */
export function getAdminBookings(status = null, driverId = null, vehicleId = null) {
  const params = {};
  if (status) params.status = status;
  if (driverId) params.driverId = driverId;
  if (vehicleId) params.vehicleId = vehicleId;
  return api.get(BOOKING_ENDPOINTS.GET_ADMIN_BOOKINGS, { params });
}

/**
 * Returns full detail of any booking (admin only, includes photo URLs).
 * @param {number} bookingId - Booking ID
 * @returns {Promise} Axios response with AdminBookingDetailResponse
 */
export function getAdminBookingDetail(bookingId) {
  return api.get(BOOKING_ENDPOINTS.GET_ADMIN_BOOKING_DETAIL(bookingId));
}
