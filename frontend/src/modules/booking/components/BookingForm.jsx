import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { validateBookingField, validateCreateBookingForm } from '../utils/bookingValidation';
import { MIN_SHIFT_HOURS, MAX_SHIFT_HOURS } from '../constants/bookingConstants';

/**
 * Form for creating a new booking.
 * Renders datetime pickers for startTime and endTime with onBlur validation.
 * @param {Object} props
 * @param {number} props.vehicleId - Pre-selected vehicle ID
 * @param {Function} props.onSubmit - Callback with { vehicleId, startTime, endTime }
 * @param {boolean} props.isLoading - Whether a submission is in progress
 */
export default function BookingForm({ vehicleId, onSubmit, isLoading = false }) {
  const now = new Date();
  // Default min datetime for the input (current time, aligned to minute)
  const minDateTime = new Date(now.getTime() + 60 * 1000).toISOString().slice(0, 16);

  const [formData, setFormData] = useState({ startTime: '', endTime: '' });
  const [errors, setErrors] = useState({});

  /** Updates a field value. */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  /** Validates a single field on blur. */
  const handleBlur = (fieldName) => {
    const error = validateBookingField(fieldName, formData[fieldName], formData);
    setErrors((prev) => ({ ...prev, [fieldName]: error }));
  };

  /** Handles form submission with full validation. */
  const handleSubmit = (e) => {
    e.preventDefault();
    const validationErrors = validateCreateBookingForm({ vehicleId, ...formData });
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    setErrors({});
    onSubmit({ vehicleId, startTime: formData.startTime, endTime: formData.endTime });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <p className="text-xs text-text-gray">
        Shifts must be between {MIN_SHIFT_HOURS} and {MAX_SHIFT_HOURS} hours.
      </p>

      {/* Start Time */}
      <div>
        <label htmlFor="startTime" className="block text-sm font-semibold text-text-dark mb-1">
          Start Time
        </label>
        <input
          id="startTime"
          type="datetime-local"
          name="startTime"
          value={formData.startTime}
          min={minDateTime}
          onChange={handleChange}
          onBlur={() => handleBlur('startTime')}
          className={`w-full px-3 py-2.5 border-2 rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-accent-orange/30 ${
            errors.startTime ? 'border-red-400' : 'border-gray-200 focus:border-accent-orange'
          }`}
        />
        {errors.startTime && (
          <p className="text-xs text-red-500 mt-1">{errors.startTime}</p>
        )}
      </div>

      {/* End Time */}
      <div>
        <label htmlFor="endTime" className="block text-sm font-semibold text-text-dark mb-1">
          End Time
        </label>
        <input
          id="endTime"
          type="datetime-local"
          name="endTime"
          value={formData.endTime}
          min={formData.startTime || minDateTime}
          onChange={handleChange}
          onBlur={() => handleBlur('endTime')}
          className={`w-full px-3 py-2.5 border-2 rounded-lg text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-accent-orange/30 ${
            errors.endTime ? 'border-red-400' : 'border-gray-200 focus:border-accent-orange'
          }`}
        />
        {errors.endTime && (
          <p className="text-xs text-red-500 mt-1">{errors.endTime}</p>
        )}
      </div>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full flex items-center justify-center gap-2 py-2.5 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
      >
        {isLoading && <Loader2 size={16} className="animate-spin" />}
        {isLoading ? 'Booking...' : 'Request Booking'}
      </button>
    </form>
  );
}
