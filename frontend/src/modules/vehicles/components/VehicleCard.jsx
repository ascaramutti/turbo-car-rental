import { Car, DollarSign } from 'lucide-react';
import {
  CATEGORY_LABELS,
  STATUS_CONFIG,
  SERVICE_TYPE_LABELS,
  SERVICE_TYPE_CONFIG,
} from '../constants/vehicleConstants';

/**
 * Displays a vehicle summary card with status badge.
 * Clicking the card navigates to the vehicle detail page.
 * @param {Object} props
 * @param {Object} props.vehicle - Vehicle object from the API
 * @param {Function} props.onClick - Callback triggered when the card is clicked
 */
export default function VehicleCard({ vehicle, onClick }) {
  const statusConfig = STATUS_CONFIG[vehicle.status];
  const categoryLabel = CATEGORY_LABELS[vehicle.category] || vehicle.category;
  const isExpired = vehicle.isActive && vehicle.availableUntil && new Date(vehicle.availableUntil) < new Date();
  const isEffectivelyActive = vehicle.isActive && !isExpired;

  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full text-left border-2 border-gray-200 rounded-xl p-5 hover:border-accent-orange hover:shadow-md transition-all bg-white"
    >
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-gray-100 rounded-lg">
            <Car size={24} className="text-text-gray" />
          </div>
          <div>
            <p className="font-bold text-text-dark">
              {vehicle.year} {vehicle.make} {vehicle.model}
            </p>
            <p className="text-xs text-text-gray mt-0.5">{vehicle.licensePlate}</p>
          </div>
        </div>

        {statusConfig && (
          <span
            className={`inline-flex items-center px-2.5 py-1 text-xs font-semibold rounded-full ${statusConfig.bg} ${statusConfig.color} ${statusConfig.border} border`}
          >
            {statusConfig.label}
          </span>
        )}
      </div>

      <div className="mt-4 flex items-center gap-4 text-sm text-text-gray">
        <span className="inline-flex items-center gap-1">
          <Car size={14} />
          {categoryLabel}
        </span>
        {isEffectivelyActive && vehicle.hourlyRate != null && (
          <span className="inline-flex items-center gap-1">
            <DollarSign size={14} />
            ${Number(vehicle.hourlyRate).toFixed(2)}/hr
          </span>
        )}
        {vehicle.serviceType && SERVICE_TYPE_CONFIG[vehicle.serviceType] && (
          <span className={`inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-full border ${SERVICE_TYPE_CONFIG[vehicle.serviceType].bg} ${SERVICE_TYPE_CONFIG[vehicle.serviceType].color} ${SERVICE_TYPE_CONFIG[vehicle.serviceType].border}`}>
            {SERVICE_TYPE_LABELS[vehicle.serviceType]}
          </span>
        )}
      </div>
    </button>
  );
}
