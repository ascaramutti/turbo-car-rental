import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, Car, AlertTriangle, DollarSign, MapPin, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { getVehicleDetail, createBooking } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import BookingForm from '../components/BookingForm';

/** Success message shown after a booking is created. */
const BOOKING_CREATED_MESSAGE = 'Booking request submitted! The owner will review it shortly.';

/**
 * Detail page for a specific vehicle in the driver's search flow.
 * Displays full vehicle info and an inline booking form.
 */
export default function DriverVehicleDetailPage() {
  const { vehicleId } = useParams();
  const navigate = useNavigate();

  const [vehicle, setVehicle] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showBookingForm, setShowBookingForm] = useState(false);
  const [isBooking, setIsBooking] = useState(false);

  /** Fetches the vehicle detail from the driver API. */
  const fetchVehicle = useCallback(async () => {
    try {
      const { data } = await getVehicleDetail(vehicleId);
      setVehicle(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
      navigate('/driver/search');
    }
  }, [vehicleId, navigate]);

  useEffect(() => {
    const load = async () => {
      await fetchVehicle();
      setIsLoading(false);
    };
    load();
  }, [fetchVehicle]);

  /** Submits the booking creation request. */
  const handleBookingSubmit = async (data) => {
    setIsBooking(true);
    try {
      await createBooking(data);
      toast.success(BOOKING_CREATED_MESSAGE);
      setShowBookingForm(false);
      navigate('/driver/bookings');
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsBooking(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  if (!vehicle) return null;

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-2xl mx-auto">
        {/* Back */}
        <button
          type="button"
          onClick={() => navigate('/driver/search')}
          className="inline-flex items-center gap-2 text-sm text-text-gray hover:text-text-dark transition-colors mb-6"
        >
          <ArrowLeft size={16} />
          Back to Search Results
        </button>

        {/* Vehicle Info */}
        <section className="bg-white border-2 border-gray-200 rounded-xl p-6 mb-4">
          <div className="flex items-start gap-3 mb-4">
            <div className="p-3 bg-gray-100 rounded-xl">
              <Car size={28} className="text-text-gray" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-text-dark">
                {vehicle.year} {vehicle.make} {vehicle.model}
              </h1>
              <p className="text-text-gray mt-0.5">{vehicle.category}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 text-sm mb-4">
            <InfoItem label="Fuel Type" value={vehicle.fuelType} />
            <InfoItem label="Service Type" value={vehicle.serviceType} />
            {vehicle.licensePlate && <InfoItem label="License Plate" value={vehicle.licensePlate} />}
            {vehicle.vin && <InfoItem label="VIN" value={vehicle.vin} />}
            <InfoItem
              label="Hourly Rate"
              value={`$${Number(vehicle.hourlyRate).toFixed(2)}/hr`}
            />
            {vehicle.availableUntil && (
              <InfoItem label="Available Until" value={new Date(vehicle.availableUntil).toLocaleDateString()} />
            )}
            <InfoItem label="Owner" value={vehicle.ownerFullName} />
            {vehicle.ownerRating != null && (
              <InfoItem label="Owner Rating" value={`${Number(vehicle.ownerRating).toFixed(1)} / 5`} />
            )}
          </div>

          {/* Location */}
          <div className="flex items-center gap-2 text-sm text-text-gray mb-4">
            <MapPin size={14} className="shrink-0" />
            <span>{vehicle.generalLocation} (approximate)</span>
          </div>

          {/* Rate highlight */}
          <div className="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-4 py-2.5 mb-4">
            <DollarSign size={16} className="text-accent-orange shrink-0" />
            <span className="text-sm font-bold text-text-dark">
              ${Number(vehicle.hourlyRate).toFixed(2)} / hour
            </span>
          </div>

          {/* Description */}
          {vehicle.description && (
            <div className="text-sm text-text-gray border-t border-gray-100 pt-4">
              {vehicle.description}
            </div>
          )}

          {/* Service type warning */}
          {vehicle.serviceTypeWarning && (
            <div className="mt-4 flex items-start gap-2 rounded-lg bg-amber-50 border border-amber-200 px-3 py-2">
              <AlertTriangle size={14} className="shrink-0 text-amber-500 mt-0.5" />
              <p className="text-xs text-amber-700">{vehicle.serviceTypeWarning}</p>
            </div>
          )}
        </section>

        {/* Booking Form */}
        {!showBookingForm ? (
          <button
            type="button"
            onClick={() => setShowBookingForm(true)}
            className="w-full flex items-center justify-center gap-2 py-3 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors"
          >
            Book This Vehicle
          </button>
        ) : (
          <div className="bg-white border-2 border-gray-200 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-bold text-text-dark">Request Booking</h2>
              <button
                type="button"
                onClick={() => setShowBookingForm(false)}
                className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <X size={18} className="text-text-gray" />
              </button>
            </div>
            <BookingForm
              vehicleId={vehicle.vehicleId}
              onSubmit={handleBookingSubmit}
              isLoading={isBooking}
            />
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Small label-value pair for the info grid.
 */
function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">{label}</p>
      <p className="text-text-dark font-medium mt-0.5">{value}</p>
    </div>
  );
}
