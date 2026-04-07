import { useState, useEffect } from 'react';
import { Loader2, DollarSign, TrendingUp, Clock, CheckCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { getOwnerEarnings } from '../api/bookingPaymentApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { PAYMENT_STATUS_CONFIG } from '../constants/bookingConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

export default function OwnerEarningsPage() {
  const [earnings, setEarnings] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    getOwnerEarnings()
      .then(({ data }) => setEarnings(data))
      .catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => setIsLoading(false));
  }, []);

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center py-20">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  if (!earnings) {
    return (
      <div className="flex-1 flex items-center justify-center py-20">
        <p className="text-text-gray">Unable to load earnings data.</p>
      </div>
    );
  }

  return (
    <div className="flex-1 bg-bg-light px-6 py-8">
      <div className="max-w-5xl mx-auto">

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-text-dark">Your Earnings</h1>
          <p className="text-text-gray text-sm mt-1">
            Earnings from completed bookings (80% of rental after platform fee).
          </p>
        </div>

        {/* Stats cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard
            icon={<DollarSign size={20} className="text-green-500" />}
            label="Total Earnings"
            value={`$${Number(earnings.totalEarnings ?? 0).toFixed(2)}`}
            bg="bg-green-50"
          />
          <StatCard
            icon={<TrendingUp size={20} className="text-purple-500" />}
            label="This Month"
            value={`$${Number(earnings.monthEarnings ?? 0).toFixed(2)}`}
            bg="bg-purple-50"
          />
          <StatCard
            icon={<Clock size={20} className="text-amber-500" />}
            label="Pending Payouts"
            value={`$${Number(earnings.pendingPayouts ?? 0).toFixed(2)}`}
            bg="bg-amber-50"
          />
          <StatCard
            icon={<CheckCircle size={20} className="text-blue-500" />}
            label="Completed Payments"
            value={earnings.completedPayments ?? 0}
            bg="bg-blue-50"
          />
        </div>

        {/* Transaction history */}
        <div className="bg-white border-2 border-gray-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <h2 className="text-base font-bold text-text-dark uppercase tracking-wide">
              Transaction History
            </h2>
          </div>

          {earnings.transactions.length === 0 ? (
            <div className="text-center py-10 text-text-gray text-sm">
              No transactions yet. Earnings will appear here after bookings are paid.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    {['Vehicle', 'Driver', 'Date', 'Gross', 'Platform Fee', 'Your Payout', 'Status'].map((col) => (
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
                  {earnings.transactions.map((tx) => {
                    const statusConfig = PAYMENT_STATUS_CONFIG[tx.status] || {};
                    return (
                      <tr key={tx.paymentId} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 text-text-dark font-medium">{tx.vehicleSummary}</td>
                        <td className="px-4 py-3 text-text-gray">{tx.driverFullName}</td>
                        <td className="px-4 py-3 text-text-gray whitespace-nowrap">
                          {formatDateTime(tx.date)}
                        </td>
                        <td className="px-4 py-3 text-text-gray">${Number(tx.grossAmount).toFixed(2)}</td>
                        <td className="px-4 py-3 text-red-500">-${Number(tx.platformFee).toFixed(2)}</td>
                        <td className="px-4 py-3 text-green-600 font-semibold">
                          ${Number(tx.ownerPayout).toFixed(2)}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex px-2 py-0.5 text-xs font-semibold rounded-full ${statusConfig.bg} ${statusConfig.color} ${statusConfig.border} border`}>
                            {statusConfig.label || tx.status}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

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
