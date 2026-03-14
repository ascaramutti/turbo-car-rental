import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import {
  VEHICLE_CATEGORIES,
  FUEL_TYPES,
  CATEGORY_LABELS,
  FUEL_TYPE_LABELS,
  VIN_LENGTH,
  MAX_VEHICLE_AGE_YEARS,
} from '../constants/vehicleConstants';
import { validateVehicleForm, validateField } from '../utils/vehicleValidation';

/** Current year used for year input constraints. */
const CURRENT_YEAR = new Date().getFullYear();

/** Minimum allowed vehicle year. */
const MIN_YEAR = CURRENT_YEAR - MAX_VEHICLE_AGE_YEARS;

/** Maximum allowed vehicle year (next year for upcoming models). */
const MAX_YEAR = CURRENT_YEAR + 1;

/** Default empty state for the vehicle form. */
const DEFAULT_FORM_STATE = {
  vin: '',
  make: '',
  model: '',
  year: '',
  licensePlate: '',
  category: '',
  fuelType: '',
  description: '',
};

/**
 * Form component for registering or editing a vehicle.
 * Uses controlled inputs with local state and validates on submit.
 * @param {Object} props
 * @param {Function} props.onSubmit - Callback with validated form data
 * @param {Object} [props.initialData] - Pre-filled data for editing
 * @param {boolean} props.isLoading - Disables form while submitting
 */
export default function VehicleForm({ onSubmit, initialData, isLoading }) {
  const [formData, setFormData] = useState(() => ({
    ...DEFAULT_FORM_STATE,
    ...initialData,
  }));
  const [errors, setErrors] = useState({});

  /** Updates a single field in the form state and clears its error. */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  /** Validates a single field on blur for immediate feedback. */
  const handleBlur = (e) => {
    const { name, value } = e.target;
    const error = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  /** Validates and submits the form. */
  const handleSubmit = (e) => {
    e.preventDefault();

    const payload = {
      ...formData,
      year: formData.year ? Number(formData.year) : null,
    };

    const validationErrors = validateVehicleForm(payload);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    onSubmit(payload);
  };

  const isEditing = Boolean(initialData);

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {/* VIN */}
      <FormField label="VIN" error={errors.vin}>
        <input
          type="text"
          name="vin"
          value={formData.vin}
          onChange={handleChange}
          onBlur={handleBlur}
          maxLength={VIN_LENGTH}
          placeholder="17-character Vehicle Identification Number"
          disabled={isLoading || isEditing}
          className={inputClasses(errors.vin, isEditing)}
        />
      </FormField>

      {/* Make & Model */}
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Make" error={errors.make}>
          <input
            type="text"
            name="make"
            value={formData.make}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="e.g. Toyota"
            disabled={isLoading}
            className={inputClasses(errors.make)}
          />
        </FormField>
        <FormField label="Model" error={errors.model}>
          <input
            type="text"
            name="model"
            value={formData.model}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="e.g. Corolla"
            disabled={isLoading}
            className={inputClasses(errors.model)}
          />
        </FormField>
      </div>

      {/* Year & License Plate */}
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Year" error={errors.year}>
          <input
            type="number"
            name="year"
            value={formData.year}
            onChange={handleChange}
            onBlur={handleBlur}
            min={MIN_YEAR}
            max={MAX_YEAR}
            placeholder={`${MIN_YEAR}–${MAX_YEAR}`}
            disabled={isLoading}
            className={inputClasses(errors.year)}
          />
        </FormField>
        <FormField label="License Plate" error={errors.licensePlate}>
          <input
            type="text"
            name="licensePlate"
            value={formData.licensePlate}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="e.g. ABC-1234"
            disabled={isLoading}
            className={inputClasses(errors.licensePlate)}
          />
        </FormField>
      </div>

      {/* Category & Fuel Type */}
      <div className="grid grid-cols-2 gap-4">
        <FormField label="Category" error={errors.category}>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={isLoading}
            className={inputClasses(errors.category)}
          >
            <option value="">Select category</option>
            {Object.values(VEHICLE_CATEGORIES).map((cat) => (
              <option key={cat} value={cat}>
                {CATEGORY_LABELS[cat]}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label="Fuel Type" error={errors.fuelType}>
          <select
            name="fuelType"
            value={formData.fuelType}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={isLoading}
            className={inputClasses(errors.fuelType)}
          >
            <option value="">Select fuel type</option>
            {Object.values(FUEL_TYPES).map((fuel) => (
              <option key={fuel} value={fuel}>
                {FUEL_TYPE_LABELS[fuel]}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      {/* Description */}
      <FormField label="Description (optional)">
        <textarea
          name="description"
          value={formData.description}
          onChange={handleChange}
          onBlur={handleBlur}
          rows={3}
          placeholder="Describe your vehicle (features, condition, etc.)"
          disabled={isLoading}
          className={inputClasses()}
        />
      </FormField>

      {/* Submit */}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-accent-orange text-white font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
      >
        {isLoading && <Loader2 size={18} className="animate-spin" />}
        {isLoading
          ? 'Saving...'
          : isEditing
            ? 'Update Vehicle'
            : 'Register Vehicle'}
      </button>
    </form>
  );
}

/**
 * Reusable form field wrapper with label and error display.
 * @param {Object} props
 * @param {string} props.label - Field label text
 * @param {string} [props.error] - Validation error message
 * @param {React.ReactNode} props.children - Input element
 */
function FormField({ label, error, children }) {
  return (
    <div>
      <label className="block text-sm font-semibold text-text-dark mb-1.5">{label}</label>
      {children}
      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
    </div>
  );
}

/**
 * Returns Tailwind classes for an input element.
 * @param {string} [error] - Validation error to show red border
 * @param {boolean} [disabled] - Whether the field is visually disabled
 * @returns {string} Tailwind class string
 */
function inputClasses(error, disabled) {
  const base = 'w-full px-4 py-2.5 border-2 rounded-xl text-sm text-text-dark focus:outline-none focus:ring-2 focus:ring-accent-orange/30 transition-colors';
  const borderColor = error ? 'border-red-300' : 'border-gray-200';
  const disabledClasses = disabled ? 'bg-gray-100 cursor-not-allowed' : '';
  return `${base} ${borderColor} ${disabledClasses}`;
}
