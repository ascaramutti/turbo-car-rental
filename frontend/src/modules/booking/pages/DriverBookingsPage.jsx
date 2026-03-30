import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, CalendarDays } from 'lucide-react';
import toast from 'react-hot-toast';
import { getDriverBookings } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import BookingCard from '../components/BookingCard';
import { BOOKING_STATUS } from '../constants/bookingConstants';

/** Status filter tabs shown at the top of the page. */
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
 * Dashboard page listing all bookings for the authenticated driver.
 * Provides status filter tabs and navigates to booking detail on click.
 */
export default function DriverBookingsPage() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState(null);

  /** Fetches driver bookings, optionally filtered by status. */
  const fetchBookings = useCallback(async (status) => {
    setIsLoading(true);
    try {
      const { data } = await getDriverBookings(status);
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
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-text-dark">My Bookings</h1>
            <p className="text-text-gray mt-1">Track all your vehicle bookings</p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/driver/search')}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors"
          >
            Find a Vehicle
          </button>
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
                onClick={() => navigate(`/driver/bookings/${booking.bookingId}`)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
