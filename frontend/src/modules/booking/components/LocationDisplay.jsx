import { MapPin, Lock, Unlock } from 'lucide-react';

/**
 * Displays a vehicle's pickup location, showing either a masked or exact position.
 * Uses the VehicleLocationResponse shape returned by GET .../vehicle-location.
 * @param {Object} props
 * @param {Object} props.location - VehicleLocationResponse from the API
 * @param {string} props.location.generalLocation - Human-readable area name
 * @param {boolean} props.location.isExactLocation - Whether exact coordinates are shown
 * @param {number} props.location.latitude - Masked or exact latitude
 * @param {number} props.location.longitude - Masked or exact longitude
 * @param {string} props.location.message - Informational message from the backend
 */
export default function LocationDisplay({ location }) {
  if (!location) return null;

  const { generalLocation, isExactLocation, latitude, longitude, message } = location;

  return (
    <div className={`rounded-xl border-2 p-4 ${isExactLocation ? 'border-green-200 bg-green-50' : 'border-amber-200 bg-amber-50'}`}>
      {/* Header */}
      <div className="flex items-center gap-2 mb-2">
        {isExactLocation ? (
          <Unlock size={16} className="text-green-600 shrink-0" />
        ) : (
          <Lock size={16} className="text-amber-600 shrink-0" />
        )}
        <span className={`text-sm font-semibold ${isExactLocation ? 'text-green-700' : 'text-amber-700'}`}>
          {isExactLocation ? 'Exact Pickup Location' : 'Approximate Location'}
        </span>
      </div>

      {/* General area */}
      <div className="flex items-center gap-1.5 text-sm text-text-dark mb-1">
        <MapPin size={14} className="shrink-0 text-text-gray" />
        <span>{generalLocation}</span>
      </div>

      {/* Coordinates — only shown when exact */}
      {isExactLocation && latitude != null && longitude != null && (
        <p className="text-xs text-text-gray mt-1">
          Coordinates: {latitude.toFixed(4)}, {longitude.toFixed(4)}
        </p>
      )}

      {/* Backend message */}
      {message && (
        <p className={`text-xs mt-2 font-medium ${isExactLocation ? 'text-green-700' : 'text-amber-700'}`}>
          {message}
        </p>
      )}
    </div>
  );
}
