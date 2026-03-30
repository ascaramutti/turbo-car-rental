import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, MapPin, X } from 'lucide-react';
import toast from 'react-hot-toast';
import {
  getDriverBookingDetail,
  cancelDriverBooking,
  startBooking,
  completeBooking,
  getVehicleLocation,
} from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { validateBookingField } from '../utils/bookingValidation';
import BookingStatusBadge from '../components/BookingStatusBadge';
import PhotoUpload from '../components/PhotoUpload';
import LocationDisplay from '../components/LocationDisplay';
import AuthImage from '../components/AuthImage';
import { BOOKING_STATUS, MAX_REASON_LENGTH } from '../constants/bookingConstants';
import { SERVICE_TYPE_LABELS, CATEGORY_LABELS } from '../../vehicles/constants/vehicleConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/** Success messages for each action. */
const SUCCESS_MESSAGES = {
  cancel: 'Booking cancelled successfully',
  start: 'Shift started! Have a safe trip.',
  complete: 'Shift completed successfully',
};

/**
 * Full booking detail page from the driver's perspective.
 * Shows booking info, conditional action buttons, location reveal, and photo upload.
 */
export default function DriverBookingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [location, setLocation] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Cancel form state
  const [showCancelForm, setShowCancelForm] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelReasonError, setCancelReasonError] = useState(null);

  // Photo upload state (arrays for multi-photo)
  const [showStartForm, setShowStartForm] = useState(false);
  const [showCompleteForm, setShowCompleteForm] = useState(false);
  const [pickupPhotos, setPickupPhotos] = useState([]);
  const [pickupPhotosError, setPickupPhotosError] = useState(null);
  const [returnPhotos, setReturnPhotos] = useState([]);
  const [returnPhotosError, setReturnPhotosError] = useState(null);

  /** Fetches the booking detail and, if confirmed/in-progress, the vehicle location. */
  const fetchBooking = useCallback(async () => {
    try {
      const { data } = await getDriverBookingDetail(id);
      setBooking(data);

      if (
        data.status === BOOKING_STATUS.CONFIRMED ||
        data.status === BOOKING_STATUS.IN_PROGRESS ||
        data.status === BOOKING_STATUS.COMPLETED
      ) {
        try {
          const { data: loc } = await getVehicleLocation(id);
          setLocation(loc);
        } catch {
          // Location fetch failure is non-critical
        }
      }
    } catch (err) {
      toast.error(extractErrorMessage(err));
      navigate('/driver/bookings');
    }
  }, [id, navigate]);

  useEffect(() => {
    const load = async () => {
      await fetchBooking();
      setIsLoading(false);
    };
    load();
  }, [fetchBooking]);

  /** Validates and submits the cancellation. */
  const handleCancel = async () => {
    const error = validateBookingField('reason', cancelReason);
    if (error) {
      setCancelReasonError(error);
      return;
    }
    setIsSubmitting(true);
    try {
      await cancelDriverBooking(id, cancelReason.trim());
      toast.success(SUCCESS_MESSAGES.cancel);
      setShowCancelForm(false);
      fetchBooking();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  /** Validates and submits the start-shift action with pickup photos. */
  const handleStart = async () => {
    if (!pickupPhotos.length) {
      setPickupPhotosError('At least one pickup photo is required to start the shift');
      return;
    }
    setIsSubmitting(true);
    try {
      await startBooking(id, pickupPhotos);
      toast.success(SUCCESS_MESSAGES.start);
      setShowStartForm(false);
      setPickupPhotos([]);
      fetchBooking();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  /** Validates and submits the complete-shift action with return photos. */
  const handleComplete = async () => {
    if (!returnPhotos.length) {
      setReturnPhotosError('At least one return photo is required to complete the shift');
      return;
    }
    setIsSubmitting(true);
    try {
      await completeBooking(id, returnPhotos);
      toast.success(SUCCESS_MESSAGES.complete);
      setShowCompleteForm(false);
      setReturnPhotos([]);
      fetchBooking();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  if (!booking) return null;

  const isTerminal = [BOOKING_STATUS.COMPLETED, BOOKING_STATUS.CANCELLED, BOOKING_STATUS.REJECTED].includes(booking.status);

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-2xl mx-auto">
        {/* Back */}
        <button
          type="button"
          onClick={() => navigate('/driver/bookings')}
          className="inline-flex items-center gap-2 text-sm text-text-gray hover:text-text-dark transition-colors mb-6"
        >
          <ArrowLeft size={16} />
          Back to My Bookings
        </button>

        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-text-dark">{booking.vehicleSummary}</h1>
            <p className="text-text-gray mt-1">Booking #{booking.bookingId}</p>
          </div>
          <BookingStatusBadge status={booking.status} />
        </div>

        {/* Booking Info */}
        <section className="bg-white border-2 border-gray-200 rounded-xl p-6 mb-4">
          <h2 className="text-base font-bold text-text-dark mb-4">Booking Details</h2>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <InfoItem label="Start Time" value={formatDateTime(booking.startTime)} />
            <InfoItem label="End Time" value={formatDateTime(booking.endTime)} />
            <InfoItem label="Total Hours" value={`${booking.totalHours}h`} />
            <InfoItem label="Total Price" value={`$${Number(booking.totalPrice).toFixed(2)}`} />
            {booking.vehicleMake && (
              <InfoItem
                label="Vehicle"
                value={`${booking.vehicleYear} ${booking.vehicleMake} ${booking.vehicleModel}`}
              />
            )}
            {booking.vehicleLicensePlate && (
              <InfoItem label="License Plate" value={booking.vehicleLicensePlate} />
            )}
            {booking.vehicleCategory && (
              <InfoItem label="Category" value={CATEGORY_LABELS[booking.vehicleCategory] || booking.vehicleCategory} />
            )}
            {booking.vehicleServiceType && (
              <InfoItem label="Service Type" value={SERVICE_TYPE_LABELS[booking.vehicleServiceType] || booking.vehicleServiceType} />
            )}
            {booking.vehicleHourlyRate && (
              <InfoItem label="Hourly Rate" value={`$${Number(booking.vehicleHourlyRate).toFixed(2)}/hr`} />
            )}
            {booking.ownerFullName && (
              <InfoItem label="Owner" value={booking.ownerFullName} />
            )}
            {booking.startedAt && (
              <InfoItem label="Started At" value={formatDateTime(booking.startedAt)} />
            )}
            {booking.completedAt && (
              <InfoItem label="Completed At" value={formatDateTime(booking.completedAt)} />
            )}
            {booking.cancelledAt && (
              <InfoItem label="Cancelled At" value={formatDateTime(booking.cancelledAt)} />
            )}
          </div>

          {/* Pickup photo grid */}
          {booking.pickupPhotoUrls && booking.pickupPhotoUrls.length > 0 && (
            <div className="mt-4 pt-4 border-t border-gray-100">
              <p className="text-xs font-semibold text-text-gray uppercase tracking-wide mb-2">
                Pickup Photos ({booking.pickupPhotoUrls.length})
              </p>
              <div className="grid grid-cols-2 gap-2">
                {booking.pickupPhotoUrls.map((url, index) => (
                  <AuthImage
                    key={url}
                    src={url}
                    alt={`Pickup photo ${index + 1}`}
                    className="w-full h-32 object-cover rounded-lg border border-gray-200"
                  />
                ))}
              </div>
            </div>
          )}

          {/* Return photo grid */}
          {booking.returnPhotoUrls && booking.returnPhotoUrls.length > 0 && (
            <div className="mt-4 pt-4 border-t border-gray-100">
              <p className="text-xs font-semibold text-text-gray uppercase tracking-wide mb-2">
                Return Photos ({booking.returnPhotoUrls.length})
              </p>
              <div className="grid grid-cols-2 gap-2">
                {booking.returnPhotoUrls.map((url, index) => (
                  <AuthImage
                    key={url}
                    src={url}
                    alt={`Return photo ${index + 1}`}
                    className="w-full h-32 object-cover rounded-lg border border-gray-200"
                  />
                ))}
              </div>
            </div>
          )}

          {/* Cancellation / rejection reason */}
          {booking.cancellationReason && (
            <div className="mt-4 pt-4 border-t border-gray-100">
              <p className="text-xs font-semibold text-text-gray uppercase tracking-wide mb-1">
                {booking.status === BOOKING_STATUS.REJECTED ? 'Rejection Reason' : 'Cancellation Reason'}
              </p>
              <p className="text-sm text-text-dark">{booking.cancellationReason}</p>
              {booking.cancelledBy && (
                <p className="text-xs text-text-gray mt-1">Cancelled by: {booking.cancelledBy}</p>
              )}
            </div>
          )}
        </section>

        {/* Location (CONFIRMED / IN_PROGRESS / COMPLETED) */}
        {location && (
          <section className="mb-4">
            <h2 className="text-base font-bold text-text-dark mb-2 flex items-center gap-2">
              <MapPin size={16} />
              Vehicle Location
            </h2>
            <LocationDisplay location={location} />
          </section>
        )}

        {/* Actions */}
        {!isTerminal && (
          <section className="bg-white border-2 border-gray-200 rounded-xl p-6 space-y-4">
            <h2 className="text-base font-bold text-text-dark">Actions</h2>

            {/* Action buttons row */}
            {!showStartForm && !showCompleteForm && !showCancelForm && (
              <div className="flex items-center gap-3">
                {booking.status === BOOKING_STATUS.CONFIRMED && (
                  <button
                    type="button"
                    onClick={() => setShowStartForm(true)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors"
                  >
                    Start Shift
                  </button>
                )}
                {booking.status === BOOKING_STATUS.IN_PROGRESS && (
                  <button
                    type="button"
                    onClick={() => setShowCompleteForm(true)}
                    className="inline-flex items-center gap-2 px-5 py-2.5 bg-green-600 text-white text-sm font-bold rounded-full hover:bg-green-700 transition-colors"
                  >
                    Complete Shift
                  </button>
                )}
                {(booking.status === BOOKING_STATUS.PENDING || booking.status === BOOKING_STATUS.CONFIRMED) && (
                  <button
                    type="button"
                    onClick={() => setShowCancelForm(true)}
                    className="inline-flex items-center gap-2 px-4 py-2 border-2 border-red-300 text-red-500 text-sm font-semibold rounded-full hover:bg-red-50 transition-colors"
                  >
                    Cancel Booking
                  </button>
                )}
              </div>
            )}

            {/* Start Shift form (CONFIRMED) */}
            {booking.status === BOOKING_STATUS.CONFIRMED && showStartForm && (
              <div className="space-y-3">
                <p className="text-sm font-semibold text-text-dark">Upload Pickup Photo</p>
                <PhotoUpload
                  label="Pickup condition photos"
                  files={pickupPhotos}
                  onFilesChange={(updatedFiles, err) => {
                    setPickupPhotos(updatedFiles);
                    setPickupPhotosError(err);
                  }}
                  error={pickupPhotosError}
                />
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleStart}
                    disabled={isSubmitting}
                    className="flex items-center gap-2 px-4 py-2 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
                  >
                    {isSubmitting && <Loader2 size={14} className="animate-spin" />}
                    Confirm Start
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowStartForm(false); setPickupPhotos([]); setPickupPhotosError(null); }}
                    className="flex items-center gap-2 px-4 py-2 border-2 border-gray-200 text-text-gray text-sm font-semibold rounded-full hover:bg-gray-50 transition-colors"
                  >
                    <X size={14} />
                    Back
                  </button>
                </div>
              </div>
            )}

            {/* Complete Shift form (IN_PROGRESS) */}
            {booking.status === BOOKING_STATUS.IN_PROGRESS && showCompleteForm && (
              <div className="space-y-3">
                <p className="text-sm font-semibold text-text-dark">Upload Return Photo</p>
                <PhotoUpload
                  label="Return condition photos"
                  files={returnPhotos}
                  onFilesChange={(updatedFiles, err) => {
                    setReturnPhotos(updatedFiles);
                    setReturnPhotosError(err);
                  }}
                  error={returnPhotosError}
                />
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleComplete}
                    disabled={isSubmitting}
                    className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white text-sm font-bold rounded-full hover:bg-green-700 transition-colors disabled:opacity-50"
                  >
                    {isSubmitting && <Loader2 size={14} className="animate-spin" />}
                    Confirm Completion
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowCompleteForm(false); setReturnPhotos([]); setReturnPhotosError(null); }}
                    className="flex items-center gap-2 px-4 py-2 border-2 border-gray-200 text-text-gray text-sm font-semibold rounded-full hover:bg-gray-50 transition-colors"
                  >
                    <X size={14} />
                    Back
                  </button>
                </div>
              </div>
            )}

            {/* Cancel Booking form */}
            {showCancelForm && (
              <div className="space-y-2">
                <label className="block text-sm font-semibold text-text-dark">
                  Cancellation Reason
                </label>
                <textarea
                  value={cancelReason}
                  onChange={(e) => {
                    setCancelReason(e.target.value);
                    if (cancelReasonError) setCancelReasonError(null);
                  }}
                  onBlur={() => {
                    const err = validateBookingField('reason', cancelReason);
                    setCancelReasonError(err);
                  }}
                  maxLength={MAX_REASON_LENGTH}
                  rows={3}
                  placeholder="Provide a reason for cancellation..."
                  className={`w-full px-3 py-2 border-2 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-red-300 ${
                    cancelReasonError ? 'border-red-400' : 'border-gray-200'
                  }`}
                />
                {cancelReasonError && (
                  <p className="text-xs text-red-500">{cancelReasonError}</p>
                )}
                <p className="text-xs text-text-gray text-right">
                  {cancelReason.length}/{MAX_REASON_LENGTH}
                </p>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={isSubmitting}
                    className="flex items-center gap-2 px-4 py-2 bg-red-500 text-white text-sm font-bold rounded-full hover:bg-red-600 transition-colors disabled:opacity-50"
                  >
                    {isSubmitting && <Loader2 size={14} className="animate-spin" />}
                    Confirm Cancellation
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowCancelForm(false); setCancelReason(''); setCancelReasonError(null); }}
                    className="flex items-center gap-2 px-4 py-2 border-2 border-gray-200 text-text-gray text-sm font-semibold rounded-full hover:bg-gray-50 transition-colors"
                  >
                    <X size={14} />
                    Back
                  </button>
                </div>
              </div>
            )}
          </section>
        )}
      </div>
    </div>
  );
}

/**
 * Small label-value pair for the info grid.
 * @param {Object} props
 * @param {string} props.label
 * @param {string} props.value
 */
function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">{label}</p>
      <p className="text-text-dark font-medium mt-0.5">{value}</p>
    </div>
  );
}
