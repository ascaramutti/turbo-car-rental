import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, CheckCircle, XCircle, X } from 'lucide-react';
import toast from 'react-hot-toast';
import {
  getOwnerBookingDetail,
  confirmBooking,
  rejectBooking,
  cancelOwnerBooking,
} from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { validateBookingField } from '../utils/bookingValidation';
import BookingStatusBadge from '../components/BookingStatusBadge';
import { BOOKING_STATUS, MAX_REASON_LENGTH } from '../constants/bookingConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/** Success messages for each owner action. */
const SUCCESS_MESSAGES = {
  confirm: 'Booking confirmed. The driver will be notified.',
  reject: 'Booking rejected.',
  cancel: 'Booking cancelled.',
};

/**
 * Full booking detail page from the car owner's perspective.
 * Shows booking info and provides confirm, reject, or cancel actions
 * based on the current booking status.
 */
export default function OwnerBookingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Reject form state
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectReasonError, setRejectReasonError] = useState(null);

  // Cancel form state
  const [showCancelForm, setShowCancelForm] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelReasonError, setCancelReasonError] = useState(null);

  /** Fetches the booking detail from the owner API. */
  const fetchBooking = useCallback(async () => {
    try {
      const { data } = await getOwnerBookingDetail(id);
      setBooking(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
      navigate('/owner/bookings');
    }
  }, [id, navigate]);

  useEffect(() => {
    const load = async () => {
      await fetchBooking();
      setIsLoading(false);
    };
    load();
  }, [fetchBooking]);

  /** Confirms the booking (no body required). */
  const handleConfirm = async () => {
    setIsSubmitting(true);
    try {
      await confirmBooking(id);
      toast.success(SUCCESS_MESSAGES.confirm);
      fetchBooking();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  /** Validates and submits the rejection with a reason. */
  const handleReject = async () => {
    const error = validateBookingField('reason', rejectReason);
    if (error) {
      setRejectReasonError(error);
      return;
    }
    setIsSubmitting(true);
    try {
      await rejectBooking(id, rejectReason.trim());
      toast.success(SUCCESS_MESSAGES.reject);
      setShowRejectForm(false);
      fetchBooking();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  /** Validates and submits the owner cancellation with a reason. */
  const handleCancel = async () => {
    const error = validateBookingField('reason', cancelReason);
    if (error) {
      setCancelReasonError(error);
      return;
    }
    setIsSubmitting(true);
    try {
      await cancelOwnerBooking(id, cancelReason.trim());
      toast.success(SUCCESS_MESSAGES.cancel);
      setShowCancelForm(false);
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
          onClick={() => navigate('/owner/bookings')}
          className="inline-flex items-center gap-2 text-sm text-text-gray hover:text-text-dark transition-colors mb-6"
        >
          <ArrowLeft size={16} />
          Back to Booking Requests
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
            <InfoItem label="Driver" value={booking.driverFullName} />
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
              <InfoItem label="Category" value={booking.vehicleCategory} />
            )}
            {booking.vehicleServiceType && (
              <InfoItem label="Service Type" value={booking.vehicleServiceType} />
            )}
            {booking.vehicleHourlyRate && (
              <InfoItem label="Hourly Rate" value={`$${Number(booking.vehicleHourlyRate).toFixed(2)}/hr`} />
            )}
            {booking.confirmedAt && (
              <InfoItem label="Confirmed At" value={formatDateTime(booking.confirmedAt)} />
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
                  <img
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
                  <img
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

        {/* Actions */}
        {!isTerminal && (
          <section className="bg-white border-2 border-gray-200 rounded-xl p-6 space-y-4">
            <h2 className="text-base font-bold text-text-dark">Actions</h2>

            {/* PENDING: Confirm + Reject */}
            {booking.status === BOOKING_STATUS.PENDING && (
              <>
                {!showRejectForm ? (
                  <div className="flex gap-3">
                    <button
                      type="button"
                      onClick={handleConfirm}
                      disabled={isSubmitting}
                      className="flex items-center gap-2 px-5 py-2.5 bg-green-600 text-white text-sm font-bold rounded-full hover:bg-green-700 transition-colors disabled:opacity-50"
                    >
                      {isSubmitting ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                      Confirm Booking
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowRejectForm(true)}
                      disabled={isSubmitting}
                      className="flex items-center gap-2 px-5 py-2.5 border-2 border-red-300 text-red-500 text-sm font-bold rounded-full hover:bg-red-50 transition-colors disabled:opacity-50"
                    >
                      <XCircle size={14} />
                      Reject Booking
                    </button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <label className="block text-sm font-semibold text-text-dark">
                      Rejection Reason
                    </label>
                    <textarea
                      value={rejectReason}
                      onChange={(e) => {
                        setRejectReason(e.target.value);
                        if (rejectReasonError) setRejectReasonError(null);
                      }}
                      onBlur={() => {
                        const err = validateBookingField('reason', rejectReason);
                        setRejectReasonError(err);
                      }}
                      maxLength={MAX_REASON_LENGTH}
                      rows={3}
                      placeholder="Provide a reason for rejection..."
                      className={`w-full px-3 py-2 border-2 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-red-300 ${
                        rejectReasonError ? 'border-red-400' : 'border-gray-200'
                      }`}
                    />
                    {rejectReasonError && (
                      <p className="text-xs text-red-500">{rejectReasonError}</p>
                    )}
                    <p className="text-xs text-text-gray text-right">
                      {rejectReason.length}/{MAX_REASON_LENGTH}
                    </p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={handleReject}
                        disabled={isSubmitting}
                        className="flex items-center gap-2 px-4 py-2 bg-red-500 text-white text-sm font-bold rounded-full hover:bg-red-600 transition-colors disabled:opacity-50"
                      >
                        {isSubmitting && <Loader2 size={14} className="animate-spin" />}
                        Confirm Rejection
                      </button>
                      <button
                        type="button"
                        onClick={() => { setShowRejectForm(false); setRejectReason(''); setRejectReasonError(null); }}
                        className="flex items-center gap-2 px-4 py-2 border-2 border-gray-200 text-text-gray text-sm font-semibold rounded-full hover:bg-gray-50 transition-colors"
                      >
                        <X size={14} />
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}

            {/* CONFIRMED: Cancel only */}
            {booking.status === BOOKING_STATUS.CONFIRMED && (
              <>
                {!showCancelForm ? (
                  <button
                    type="button"
                    onClick={() => setShowCancelForm(true)}
                    className="inline-flex items-center gap-2 px-4 py-2 border-2 border-red-300 text-red-500 text-sm font-semibold rounded-full hover:bg-red-50 transition-colors"
                  >
                    Cancel Booking
                  </button>
                ) : (
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
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </>
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
