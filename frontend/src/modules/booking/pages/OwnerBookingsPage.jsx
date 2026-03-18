import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, CalendarDays } from 'lucide-react';
import toast from 'react-hot-toast';
import { getOwnerBookings } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import BookingCard from '../components/BookingCard';
import { BOOKING_STATUS } from '../constants/bookingConstants';

/** Status filter tabs for the owner bookings list. */
const STATUS_TABS = [
  { label: 'All', value: null },
  { label: 'Pending', value: BOOKING_STATUS.PENDING },
  { label: 'Confirmed', value: BOOKING_STATUS.CONFIRMED },
  { label: 'In Progress', value: BOOKING_STATUS.IN_PROGRESS },
  { label: 'Completed', value: BOOKING_STATUS.COMPLETED },
  { label: 'Cancelled', value: BOOKING_STATUS.CANCELLED },
  { label: 'Rejected', value: BOOKING_STATUS.REJECTED },
];

/** Empty-state message when no bookings match the selected filter. */
const EMPTY_STATE_MESSAGE = 'No bookings found for the selected status.';

/**
 * Page listing all bookings for vehicles owned by the authenticated car owner.
 * Provides status filter tabs and quick-action confirm/reject/cancel from the list.
 */
export default function OwnerBookingsPage() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState(null);

  /** Fetches owner's bookings, optionally filtered by status. */
  const fetchBookings = useCallback(async (status) => {
    setIsLoading(true);
    try {
      const { data } = await getOwnerBookings(status);
      setBookings(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBookings(activeStatus);
  }, [fetchBookings, activeStatus]);

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-text-dark">Booking Requests</h1>
          <p className="text-text-gray mt-1">Review and manage bookings for your vehicles</p>
        </div>

        {/* Status filter tabs */}
        <div className="flex gap-2 flex-wrap mb-6">
          {STATUS_TABS.map((tab) => (
            <button
              key={String(tab.value)}
              type="button"
              onClick={() => setActiveStatus(tab.value)}
              className={`px-4 py-1.5 rounded-full text-sm font-semibold border-2 transition-colors ${
                activeStatus === tab.value
                  ? 'bg-accent-orange text-white border-accent-orange'
                  : 'border-gray-200 text-text-gray hover:border-accent-orange'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Content */}
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Loader2 size={32} className="animate-spin text-accent-orange" />
          </div>
        ) : bookings.length === 0 ? (
          <div className="text-center py-16">
            <CalendarDays size={48} className="mx-auto text-gray-300 mb-4" />
            <p className="text-text-gray">{EMPTY_STATE_MESSAGE}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {bookings.map((booking) => (
              <BookingCard
                key={booking.bookingId}
                booking={booking}
                onClick={() => navigate(`/owner/bookings/${booking.bookingId}`)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
