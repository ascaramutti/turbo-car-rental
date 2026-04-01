import { Car, MapPin, AlertTriangle, Star } from 'lucide-react';
import { SERVICE_TYPE_LABELS } from '../../vehicles/constants/vehicleConstants';

/**
 * Displays a vehicle card in the search results listing.
 * Layout matches the wireframe: image placeholder, listing details, "View" button.
 *
 * @param {Object} props
 * @param {Object} props.vehicle - VehicleSearchResponse from the API
 * @param {Function} props.onClick - Opens the vehicle detail modal
 */
export default function VehicleSearchCard({ vehicle, onClick }) {
  return (
    <div className="bg-white border-2 border-gray-200 rounded-xl overflow-hidden hover:border-accent-orange hover:shadow-md transition-all flex flex-col">
      {/* Car image placeholder */}
      <div className="w-full h-36 bg-gray-100 flex items-center justify-center shrink-0">
        <Car size={40} className="text-gray-300" />
      </div>

      {/* Listing details */}
      <div className="flex-1 p-4 flex flex-col gap-1.5">
        {/* 1. Car Model and Year */}
        <p className="font-bold text-text-dark text-sm">
          {vehicle.year} {vehicle.make} {vehicle.model}
        </p>

        {/* 2. Service Type */}
        <p className="text-xs text-text-gray">
          <span className="font-semibold text-text-dark">Service:</span>{' '}
          {SERVICE_TYPE_LABELS[vehicle.effectiveServiceType] || SERVICE_TYPE_LABELS[vehicle.serviceType] || vehicle.serviceType}
        </p>

        {/* 3. Availability / Location */}
        <div className="flex items-center gap-1 text-xs text-text-gray">
          <MapPin size={12} className="shrink-0" />
          <span>{vehicle.generalLocation}</span>
        </div>

        {/* 4. Owner + Ratings */}
        <div className="flex items-center gap-1.5 text-xs text-text-gray">
          <span>
            Owner:{' '}
            <span className="font-medium text-text-dark">{vehicle.ownerFullName}</span>
          </span>
          {vehicle.ownerRating != null && (
            <span className="flex items-center gap-0.5 text-turbo-yellow font-semibold">
              <Star size={11} fill="currentColor" />
              {Number(vehicle.ownerRating).toFixed(1)}
            </span>
          )}
        </div>

        {/* 5. Price Per Shift */}
        <div>
          <p className="text-sm font-bold text-accent-orange">
            ${Number(vehicle.hourlyRate).toFixed(2)}{' '}
            <span className="text-xs font-normal text-text-gray">/ hr</span>
          </p>
          <p className="text-[10px] text-text-gray italic">Includes service fee</p>
        </div>

        {/* Service type warning */}
        {vehicle.serviceTypeWarning && (
          <div className="flex items-start gap-1.5 rounded-lg bg-amber-50 border border-amber-200 px-2 py-1.5 mt-1">
            <AlertTriangle size={12} className="shrink-0 text-amber-500 mt-0.5" />
            <p className="text-xs text-amber-700">{vehicle.serviceTypeWarning}</p>
          </div>
        )}
      </div>

      {/* View button */}
      <div className="px-4 pb-4">
        <button
          type="button"
          onClick={onClick}
          className="w-full py-2 bg-blue-600 text-white text-sm font-bold rounded-full hover:bg-blue-700 transition-colors"
        >
          View
        </button>
      </div>
    </div>
  );
}
