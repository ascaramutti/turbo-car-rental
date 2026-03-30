import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Car, MapPin, DollarSign, AlertTriangle, Loader2, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import { getVehicleDetail, createBooking } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import BookingForm from './BookingForm';
import { SERVICE_TYPE_LABELS, CATEGORY_LABELS, FUEL_TYPE_LABELS } from '../../vehicles/constants/vehicleConstants';

/** Success message after booking is created. */
const BOOKING_CREATED_MESSAGE = 'Booking request submitted! The owner will review it shortly.';

/**
 * Modal overlay that shows full vehicle detail when a driver clicks "View" on a search card.
 * Includes car info, owner rating, price breakdown, and inline booking form.
 *
 * @param {Object} props
 * @param {number} props.vehicleId - Vehicle to load
 * @param {Function} props.onClose - Close the modal
 */
export default function VehicleDetailModal({ vehicleId, onClose }) {
  const navigate = useNavigate();
  const [vehicle, setVehicle] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showBookingForm, setShowBookingForm] = useState(false);
  const [isBooking, setIsBooking] = useState(false);

  /** Loads vehicle detail from the API. */
  const fetchVehicle = useCallback(async () => {
    try {
      const { data } = await getVehicleDetail(vehicleId);
      setVehicle(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
      onClose();
    } finally {
      setIsLoading(false);
    }
  }, [vehicleId, onClose]);

  useEffect(() => {
    fetchVehicle();
  }, [fetchVehicle]);

  /** Submits the booking creation request. */
  const handleBookingSubmit = async (data) => {
    setIsBooking(true);
    try {
      await createBooking(data);
      toast.success(BOOKING_CREATED_MESSAGE);
      onClose();
      navigate('/driver/bookings');
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsBooking(false);
    }
  };

  /** Close on backdrop click. */
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) onClose();
  };

  return (
    <div
      className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4"
      onClick={handleBackdropClick}
    >
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl max-h-[90vh] overflow-y-auto">
        {/* Modal header */}
        <div className="flex items-center justify-between p-5 border-b border-gray-100 sticky top-0 bg-white z-10">
          <h2 className="text-lg font-bold text-text-dark">Vehicle Detail</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X size={20} className="text-text-gray" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5">
          {isLoading ? (
            <div className="flex justify-center py-12">
              <Loader2 size={32} className="animate-spin text-accent-orange" />
            </div>
          ) : vehicle ? (
            <>
              {/* Car image placeholder */}
              <div className="w-full h-44 bg-gray-100 rounded-xl flex items-center justify-center mb-5">
                <Car size={48} className="text-gray-300" />
              </div>

              {/* Car title */}
              <h3 className="text-xl font-bold text-text-dark mb-1">
                {vehicle.year} {vehicle.make} {vehicle.model}
              </h3>
              <p className="text-sm text-text-gray mb-4">{CATEGORY_LABELS[vehicle.category] || vehicle.category}</p>

              {/* Info grid */}
              <div className="grid grid-cols-2 gap-3 text-sm mb-4">
                <InfoItem label="Owner" value={vehicle.ownerFullName} />
                {vehicle.ownerRating != null && (
                  <div>
                    <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">Rating</p>
                    <div className="flex items-center gap-1 mt-0.5">
                      <Star size={14} fill="currentColor" className="text-turbo-yellow" />
                      <span className="font-medium text-text-dark">
                        {Number(vehicle.ownerRating).toFixed(1)} / 5
                      </span>
                    </div>
                  </div>
                )}
                <InfoItem label="Year" value={String(vehicle.year)} />
                <InfoItem label="Service Type" value={SERVICE_TYPE_LABELS[vehicle.effectiveServiceType] || SERVICE_TYPE_LABELS[vehicle.serviceType] || vehicle.serviceType} />
                <InfoItem label="Category" value={CATEGORY_LABELS[vehicle.category] || vehicle.category} />
                <InfoItem label="Fuel Type" value={FUEL_TYPE_LABELS[vehicle.fuelType] || vehicle.fuelType} />
              </div>

              {/* Location */}
              {vehicle.generalLocation && (
                <div className="flex items-center gap-2 text-sm text-text-gray mb-4">
                  <MapPin size={14} className="shrink-0 text-accent-orange" />
                  <span>{vehicle.generalLocation} (approximate)</span>
                </div>
              )}

              {/* Description */}
              {vehicle.description && (
                <p className="text-sm text-text-gray mb-4 border-t border-gray-100 pt-4">
                  {vehicle.description}
                </p>
              )}

              {/* Price highlight */}
              <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-4 py-3 mb-4">
                <DollarSign size={18} className="text-accent-orange shrink-0" />
                <div>
                  <p className="text-base font-bold text-text-dark">
                    ${Number(vehicle.hourlyRate).toFixed(2)} / hour
                  </p>
                  <p className="text-xs text-text-gray">Billed per hour of your shift</p>
                </div>
              </div>

              {/* Service type warning */}
              {vehicle.serviceTypeWarning && (
                <div className="flex items-start gap-2 rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 mb-4">
                  <AlertTriangle size={14} className="shrink-0 text-amber-500 mt-0.5" />
                  <p className="text-xs text-amber-700">{vehicle.serviceTypeWarning}</p>
                </div>
              )}

              {/* Booking section */}
              {!showBookingForm ? (
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => setShowBookingForm(true)}
                    className="flex-1 py-3 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors uppercase tracking-wide"
                  >
                    Book This Car
                  </button>
                  <button
                    type="button"
                    onClick={onClose}
                    className="flex-1 py-3 border-2 border-gray-200 text-text-gray text-sm font-semibold rounded-full hover:bg-gray-50 transition-colors"
                  >
                    View More Options
                  </button>
                </div>
              ) : (
                <div className="border-2 border-gray-200 rounded-xl p-4">
                  <div className="flex items-center justify-between mb-4">
                    <h4 className="text-base font-bold text-text-dark">Request Booking</h4>
                    <button
                      type="button"
                      onClick={() => setShowBookingForm(false)}
                      className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                      <X size={16} className="text-text-gray" />
                    </button>
                  </div>
                  <BookingForm
                    vehicleId={vehicle.vehicleId}
                    availableUntil={vehicle.availableUntil}
                    onSubmit={handleBookingSubmit}
                    isLoading={isBooking}
                  />
                </div>
              )}
            </>
          ) : null}
        </div>
      </div>
    </div>
  );
}

/**
 * Small label-value pair for the info grid inside the modal.
 */
function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">{label}</p>
      <p className="text-text-dark font-medium mt-0.5">{value}</p>
    </div>
  );
}
