import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, CalendarDays } from 'lucide-react';
import toast from 'react-hot-toast';
import { getAdminBookings } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import BookingStatusBadge from '../components/BookingStatusBadge';
import { BOOKING_STATUS } from '../constants/bookingConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/** Status filter tabs. */
const STATUS_TABS = [
  { label: 'All', value: null },
  { label: 'Pending', value: BOOKING_STATUS.PENDING },
  { label: 'Confirmed', value: BOOKING_STATUS.CONFIRMED },
  { label: 'In Progress', value: BOOKING_STATUS.IN_PROGRESS },
  { label: 'Completed', value: BOOKING_STATUS.COMPLETED },
  { label: 'Cancelled', value: BOOKING_STATUS.CANCELLED },
  { label: 'Rejected', value: BOOKING_STATUS.REJECTED },
];

/**
 * Admin bookings page — read-only overview of all platform bookings.
 * Filterable by status. Clicking a row navigates to booking detail.
 */
export default function AdminBookingsPage() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeStatus, setActiveStatus] = useState(null);

  const fetchBookings = useCallback(async (status) => {
    setIsLoading(true);
    try {
      const { data } = await getAdminBookings(status);
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
    <div className="flex-1 bg-bg-light px-6 py-8">
      <div className="max-w-6xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-text-dark uppercase tracking-wide">
            All Bookings
          </h1>
          <p className="text-text-gray mt-1 text-sm">
            Platform-wide booking overview for dispute resolution and monitoring
          </p>
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

        {isLoading ? (
          <div className="flex justify-center py-12">
            <Loader2 size={32} className="animate-spin text-accent-orange" />
          </div>
        ) : bookings.length === 0 ? (
          <div className="text-center py-16">
            <CalendarDays size={48} className="mx-auto text-gray-300 mb-4" />
            <p className="text-text-gray">No bookings found for the selected status.</p>
          </div>
        ) : (
          <div className="bg-white border-2 border-gray-200 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['ID', 'Driver', 'Owner', 'Vehicle', 'Date & Time', 'Duration', 'Total', 'Status'].map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-bold text-text-gray uppercase tracking-wide whitespace-nowrap"
                      >
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {bookings.map((booking) => (
                    <tr
                      key={booking.bookingId}
                      onClick={() => navigate(`/admin/bookings/${booking.bookingId}`)}
                      className="hover:bg-gray-50 transition-colors cursor-pointer"
                    >
                      <td className="px-4 py-3 text-text-gray font-mono text-xs">
                        #{booking.bookingId}
                      </td>
                      <td className="px-4 py-3 text-text-dark font-medium whitespace-nowrap">
                        {booking.driverFullName ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-text-dark whitespace-nowrap">
                        {booking.ownerFullName ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-text-gray">
                        {booking.vehicleSummary}
                      </td>
                      <td className="px-4 py-3 text-text-gray whitespace-nowrap">
                        {formatDateTime(booking.startTime)}
                      </td>
                      <td className="px-4 py-3 text-text-gray whitespace-nowrap">
                        {booking.totalHours}h
                      </td>
                      <td className="px-4 py-3 text-text-dark font-semibold whitespace-nowrap">
                        ${Number(booking.totalPrice).toFixed(2)}
                      </td>
                      <td className="px-4 py-3">
                        <BookingStatusBadge status={booking.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
