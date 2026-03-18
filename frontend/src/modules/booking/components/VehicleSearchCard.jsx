import { Car, DollarSign, MapPin, AlertTriangle } from 'lucide-react';

/**
 * Displays a vehicle card in search results.
 * Shows make/model/year, hourly rate, masked location, and a service-type warning banner.
 * @param {Object} props
 * @param {Object} props.vehicle - VehicleSearchResponse from the API
 * @param {Function} props.onClick - Callback triggered when the card is clicked
 */
export default function VehicleSearchCard({ vehicle, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full text-left border-2 border-gray-200 rounded-xl p-5 hover:border-accent-orange hover:shadow-md transition-all bg-white"
    >
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-gray-100 rounded-lg">
            <Car size={22} className="text-text-gray" />
          </div>
          <div>
            <p className="font-bold text-text-dark">
              {vehicle.year} {vehicle.make} {vehicle.model}
            </p>
            <p className="text-xs text-text-gray mt-0.5">{vehicle.category}</p>
          </div>
        </div>
        <div className="flex items-center gap-1.5 text-sm font-bold text-text-dark">
          <DollarSign size={15} className="text-accent-orange" />
          ${Number(vehicle.hourlyRate).toFixed(2)}/hr
        </div>
      </div>

      {/* Location */}
      <div className="mt-3 flex items-center gap-1.5 text-sm text-text-gray">
        <MapPin size={14} className="shrink-0" />
        <span>{vehicle.generalLocation}</span>
      </div>

      {/* Owner info */}
      <div className="mt-1 text-xs text-text-gray">
        Owner: <span className="font-medium text-text-dark">{vehicle.ownerFullName}</span>
        {vehicle.ownerRating != null && (
          <span className="ml-2">&#9733; {Number(vehicle.ownerRating).toFixed(1)}</span>
        )}
      </div>

      {/* Effective service type */}
      <div className="mt-2 text-xs text-text-gray">
        Service: <span className="font-medium text-text-dark">{vehicle.effectiveServiceType || vehicle.serviceType}</span>
      </div>

      {/* Service type warning banner */}
      {vehicle.serviceTypeWarning && (
        <div className="mt-3 flex items-start gap-2 rounded-lg bg-amber-50 border border-amber-200 px-3 py-2">
          <AlertTriangle size={14} className="shrink-0 text-amber-500 mt-0.5" />
          <p className="text-xs text-amber-700">{vehicle.serviceTypeWarning}</p>
        </div>
      )}
    </button>
  );
}
