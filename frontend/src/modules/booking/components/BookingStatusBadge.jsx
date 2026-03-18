import { STATUS_CONFIG } from '../constants/bookingConstants';

/**
 * Colored badge that displays a booking's current status.
 * Styling follows the STATUS_CONFIG map in bookingConstants.
 * @param {Object} props
 * @param {string} props.status - BookingStatus value from the backend
 */
export default function BookingStatusBadge({ status }) {
  const config = STATUS_CONFIG[status];

  if (!config) {
    return (
      <span className="inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full bg-gray-100 text-gray-500 border border-gray-200">
        {status}
      </span>
    );
  }

  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full border ${config.bg} ${config.color} ${config.border}`}
    >
      {config.label}
    </span>
  );
}
