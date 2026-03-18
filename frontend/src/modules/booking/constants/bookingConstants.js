/**
 * Booking module constants matching the backend API contract (Module 4).
 */

/** Booking status enum values matching BookingStatus on the backend. */
export const BOOKING_STATUS = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
  REJECTED: 'REJECTED',
};

/** UI styling configuration for each booking status. */
export const STATUS_CONFIG = {
  [BOOKING_STATUS.PENDING]: {
    label: 'Pending',
    color: 'text-amber-600',
    bg: 'bg-amber-50',
    border: 'border-amber-200',
  },
  [BOOKING_STATUS.CONFIRMED]: {
    label: 'Confirmed',
    color: 'text-blue-600',
    bg: 'bg-blue-50',
    border: 'border-blue-200',
  },
  [BOOKING_STATUS.IN_PROGRESS]: {
    label: 'In Progress',
    color: 'text-purple-600',
    bg: 'bg-purple-50',
    border: 'border-purple-200',
  },
  [BOOKING_STATUS.COMPLETED]: {
    label: 'Completed',
    color: 'text-green-600',
    bg: 'bg-green-50',
    border: 'border-green-200',
  },
  [BOOKING_STATUS.CANCELLED]: {
    label: 'Cancelled',
    color: 'text-gray-500',
    bg: 'bg-gray-50',
    border: 'border-gray-200',
  },
  [BOOKING_STATUS.REJECTED]: {
    label: 'Rejected',
    color: 'text-red-500',
    bg: 'bg-red-50',
    border: 'border-red-200',
  },
};

/** Minimum shift duration in hours (matches BookingValidationConstraints.MIN_SHIFT_HOURS). */
export const MIN_SHIFT_HOURS = 4;

/** Maximum shift duration in hours (matches BookingValidationConstraints.MAX_SHIFT_HOURS). */
export const MAX_SHIFT_HOURS = 24;

/** Minutes before startTime that driver can begin check-in (matches EARLY_START_MINUTES). */
export const EARLY_START_MINUTES = 15;

/** Hours after startTime that start window expires (matches LATE_START_HOURS). */
export const LATE_START_HOURS = 1;

/** Hours before startTime when exact location is revealed (matches LOCATION_REVEAL_HOURS). */
export const LOCATION_REVEAL_HOURS = 2;

/** Maximum length for cancellation/rejection reason (matches MAX_REASON_LENGTH). */
export const MAX_REASON_LENGTH = 500;

/** All booking API endpoint paths. */
export const BOOKING_ENDPOINTS = {
  // Driver — vehicle search
  SEARCH_VEHICLES: '/driver/bookings/vehicles/search',
  VEHICLE_DETAIL: (vehicleId) => `/driver/bookings/vehicles/${vehicleId}`,

  // Driver — booking CRUD
  CREATE_BOOKING: '/driver/bookings',
  GET_DRIVER_BOOKINGS: '/driver/bookings',
  GET_DRIVER_BOOKING_DETAIL: (bookingId) => `/driver/bookings/${bookingId}`,
  CANCEL_DRIVER_BOOKING: (bookingId) => `/driver/bookings/${bookingId}/cancel`,
  START_BOOKING: (bookingId) => `/driver/bookings/${bookingId}/start`,
  COMPLETE_BOOKING: (bookingId) => `/driver/bookings/${bookingId}/complete`,
  GET_VEHICLE_LOCATION: (bookingId) => `/driver/bookings/${bookingId}/vehicle-location`,

  // Owner — booking management
  GET_OWNER_BOOKINGS: '/owner/bookings',
  GET_OWNER_BOOKING_DETAIL: (bookingId) => `/owner/bookings/${bookingId}`,
  CONFIRM_BOOKING: (bookingId) => `/owner/bookings/${bookingId}/confirm`,
  REJECT_BOOKING: (bookingId) => `/owner/bookings/${bookingId}/reject`,
  CANCEL_OWNER_BOOKING: (bookingId) => `/owner/bookings/${bookingId}/cancel`,

  // Admin — read-only overview
  GET_ADMIN_BOOKINGS: '/admin/bookings',
  GET_ADMIN_BOOKING_DETAIL: (bookingId) => `/admin/bookings/${bookingId}`,
};

/** User-facing error messages corresponding to backend BOOK-XXX codes. */
export const BOOKING_ERROR_MESSAGES = {
  'BOOK-001': 'Driver must be verified before creating bookings',
  'BOOK-002': 'Vehicle is not approved for rentals',
  'BOOK-003': 'Vehicle is not currently available for rent',
  'BOOK-004': 'Booking end time exceeds vehicle availability window',
  'BOOK-005': 'Booking start time must be in the future',
  'BOOK-006': 'Booking end time must be after start time',
  'BOOK-007': 'Minimum booking duration is 4 hours',
  'BOOK-008': 'Vehicle is already booked for the requested time slot',
  'BOOK-009': 'You cannot book your own vehicle',
  'BOOK-010': 'You already have an active booking during this time slot',
  'BOOK-011': 'Booking not found',
  'BOOK-012': 'You do not have permission to access this booking',
  'BOOK-013': 'Only pending bookings can be confirmed',
  'BOOK-014': 'Only pending bookings can be rejected',
  'BOOK-015': 'Only pending or confirmed bookings can be cancelled',
  'BOOK-016': 'Only confirmed bookings can be started',
  'BOOK-017': 'Booking cannot be started yet (too early)',
  'BOOK-018': 'Booking start window has expired',
  'BOOK-019': 'Only in-progress bookings can be completed',
  'BOOK-020': 'At least one pickup photo is required to start the shift',
  'BOOK-021': 'At least one return photo is required to complete the shift',
  'BOOK-022': 'Cancellation reason is required',
  'BOOK-023': 'Rejection reason is required',
  'BOOK-025': 'Maximum booking duration is 24 hours',
  'BOOK-026': 'Photo upload limit exceeded. Maximum 10 photos allowed per upload',
};

/** Accepted photo file formats for pickup/return photos. */
export const ACCEPTED_PHOTO_FORMATS = '.jpg,.jpeg,.png';

/** Maximum photo file size in bytes (5MB). */
export const MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024;

/** Maximum photo file size label for display. */
export const MAX_PHOTO_SIZE_LABEL = '5MB';

/** Minimum number of photos required per upload (pickup or return). */
export const MIN_PHOTOS_PER_UPLOAD = 1;

/** Maximum number of photos allowed per upload (pickup or return). */
export const MAX_PHOTOS_PER_UPLOAD = 10;
