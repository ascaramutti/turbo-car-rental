import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { getAdminBookingDetail } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import AuthImage from '../components/AuthImage';
import BookingStatusBadge from '../components/BookingStatusBadge';
import { BOOKING_STATUS } from '../constants/bookingConstants';
import { SERVICE_TYPE_LABELS, CATEGORY_LABELS } from '../../vehicles/constants/vehicleConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/**
 * Admin booking detail page — read-only view with full info and photos.
 * Used for dispute resolution and platform monitoring.
 */
export default function AdminBookingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchBooking = useCallback(async () => {
    try {
      const { data } = await getAdminBookingDetail(id);
      setBooking(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
      navigate('/admin/bookings');
    } finally {
      setIsLoading(false);
    }
  }, [id, navigate]);

  useEffect(() => {
    fetchBooking();
  }, [fetchBooking]);

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  if (!booking) return null;

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-2xl mx-auto">
        <button
          type="button"
          onClick={() => navigate('/admin/bookings')}
          className="inline-flex items-center gap-2 text-sm text-text-gray hover:text-text-dark transition-colors mb-6"
        >
          <ArrowLeft size={16} />
          Back to All Bookings
        </button>

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
            {booking.ownerFullName && (
              <InfoItem label="Owner" value={booking.ownerFullName} />
            )}
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

          {/* Pickup photos */}
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

          {/* Return photos */}
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
      </div>
    </div>
  );
}

function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">{label}</p>
      <p className="text-text-dark font-medium mt-0.5">{value}</p>
    </div>
  );
}
