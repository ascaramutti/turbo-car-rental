import { Car, Clock, DollarSign } from 'lucide-react';
import { formatDateTime } from '../../../shared/utils/dateUtils';
import BookingStatusBadge from './BookingStatusBadge';

/**
 * Displays a booking summary card for use in list views.
 * Shows status badge, vehicle summary, shift times, and total price.
 * @param {Object} props
 * @param {Object} props.booking - BookingResponse object from the API
 * @param {Function} props.onClick - Callback triggered when the card is clicked
 */
export default function BookingCard({ booking, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full text-left border-2 border-gray-200 rounded-xl p-5 hover:border-accent-orange hover:shadow-md transition-all bg-white"
    >
      {/* Header: vehicle summary + status */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-gray-100 rounded-lg">
            <Car size={22} className="text-text-gray" />
          </div>
          <div>
            <p className="font-bold text-text-dark">{booking.vehicleSummary}</p>
            <p className="text-xs text-text-gray mt-0.5">
              Booking #{booking.bookingId}
            </p>
          </div>
        </div>
        <BookingStatusBadge status={booking.status} />
      </div>

      {/* Shift times and price */}
      <div className="mt-4 grid grid-cols-2 gap-3 text-sm text-text-gray">
        <div className="flex items-center gap-1.5">
          <Clock size={14} className="shrink-0" />
          <span>
            {formatDateTime(booking.startTime)}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <Clock size={14} className="shrink-0" />
          <span>
            {formatDateTime(booking.endTime)}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <DollarSign size={14} className="shrink-0" />
          <span>
            ${Number(booking.totalPrice).toFixed(2)} &middot; {booking.totalHours}h
          </span>
        </div>
        {booking.driverFullName && (
          <div className="text-xs text-text-gray">
            Driver: <span className="font-medium text-text-dark">{booking.driverFullName}</span>
          </div>
        )}
      </div>
    </button>
  );
}
