import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Loader2, Car, DollarSign, TrendingUp, Star, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import { getOwnerDashboardStats, getOwnerBookings } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { useAuth } from '../../auth/context/useAuth';
import BookingStatusBadge from '../components/BookingStatusBadge';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/**
 * Owner dashboard page.
 * Shows a welcome banner, stats cards, and a recent bookings table.
 */
export default function OwnerDashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [recentBookings, setRecentBookings] = useState([]);
  const [isLoadingStats, setIsLoadingStats] = useState(true);
  const [isLoadingBookings, setIsLoadingBookings] = useState(true);
  const [warningMessage, setWarningMessage] = useState(null);

  useEffect(() => {
    getOwnerDashboardStats()
      .then(({ data }) => {
        setStats(data);
        if (data?.warning) setWarningMessage(data.warning);
      })
      .catch(() => {
        // Dashboard stats endpoint may not be implemented yet — show zeros
        setStats({ activeVehicles: 0, totalEarnings: 0, monthEarnings: 0, rating: null });
      })
      .finally(() => setIsLoadingStats(false));
  }, []);

  useEffect(() => {
    getOwnerBookings()
      .then(({ data }) => setRecentBookings(data.slice(0, 5)))
      .catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => setIsLoadingBookings(false));
  }, []);

  return (
    <div className="flex-1 bg-bg-light px-6 py-8">
      <div className="max-w-5xl mx-auto">

        {/* Welcome banner */}
        <div className="bg-white border-2 border-gray-200 rounded-xl p-5 mb-4">
          <h1 className="text-2xl font-bold text-text-dark">
            Welcome back, {user?.firstName}!
          </h1>
          <p className="text-text-gray mt-1 text-sm">
            Here is an overview of your vehicles and bookings.
          </p>
        </div>

        {/* Warning banner (conditional) */}
        {warningMessage && (
          <div className="flex items-start gap-3 bg-red-50 border-2 border-red-200 rounded-xl p-4 mb-4">
            <AlertTriangle size={18} className="shrink-0 text-red-500 mt-0.5" />
            <p className="text-sm text-red-700 font-medium">{warningMessage}</p>
          </div>
        )}

        {/* Stats cards row */}
        {isLoadingStats ? (
          <div className="flex justify-center py-8">
            <Loader2 size={28} className="animate-spin text-accent-orange" />
          </div>
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
            <StatCard
              icon={<Car size={20} className="text-blue-500" />}
              label="Active Vehicles"
              value={stats?.activeVehicles ?? 0}
              bg="bg-blue-50"
            />
            <StatCard
              icon={<DollarSign size={20} className="text-green-500" />}
              label="Total Earnings"
              value={`$${Number(stats?.totalEarnings ?? 0).toFixed(2)}`}
              bg="bg-green-50"
            />
            <StatCard
              icon={<TrendingUp size={20} className="text-purple-500" />}
              label="This Month"
              value={`$${Number(stats?.monthEarnings ?? 0).toFixed(2)}`}
              bg="bg-purple-50"
            />
            <StatCard
              icon={<Star size={20} className="text-turbo-yellow" />}
              label="Your Rating"
              value={stats?.rating != null ? `${Number(stats.rating).toFixed(1)} / 5` : 'N/A'}
              bg="bg-yellow-50"
            />
          </div>
        )}

        {/* Recent Bookings section */}
        <div className="bg-white border-2 border-gray-200 rounded-xl overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h2 className="text-base font-bold text-text-dark uppercase tracking-wide">
              Recent Bookings
            </h2>
            <Link
              to="/owner/bookings"
              className="text-sm font-semibold text-blue-600 hover:text-blue-700 transition-colors"
            >
              View All
            </Link>
          </div>

          {isLoadingBookings ? (
            <div className="flex justify-center py-10">
              <Loader2 size={24} className="animate-spin text-accent-orange" />
            </div>
          ) : recentBookings.length === 0 ? (
            <div className="text-center py-10 text-text-gray text-sm">
              No bookings yet.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    {['Driver', 'Vehicle', 'Date & Time', 'Duration', 'Earnings', 'Status'].map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-bold text-text-gray uppercase tracking-wide"
                      >
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {recentBookings.map((booking) => (
                    <tr key={booking.bookingId} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 text-text-dark font-medium">
                        {booking.driverFullName ?? '—'}
                      </td>
                      <td className="px-4 py-3 text-text-gray">{booking.vehicleSummary}</td>
                      <td className="px-4 py-3 text-text-gray whitespace-nowrap">
                        {formatDateTime(booking.startTime)}
                      </td>
                      <td className="px-4 py-3 text-text-gray">{booking.totalHours}h</td>
                      <td className="px-4 py-3 text-text-dark font-semibold">
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
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * Individual stat card for the dashboard overview row.
 * @param {Object} props
 * @param {React.ReactNode} props.icon
 * @param {string} props.label
 * @param {string|number} props.value
 * @param {string} props.bg - Tailwind background class
 */
function StatCard({ icon, label, value, bg }) {
  return (
    <div className={`${bg} border-2 border-gray-100 rounded-xl p-4 flex flex-col gap-2`}>
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-xs font-semibold text-text-gray uppercase tracking-wide">{label}</span>
      </div>
      <p className="text-2xl font-bold text-text-dark">{value}</p>
    </div>
  );
}
