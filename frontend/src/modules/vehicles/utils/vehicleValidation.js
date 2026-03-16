import {
  VIN_LENGTH,
  MIN_HOURLY_RATE,
  MAX_VEHICLE_AGE_YEARS,
} from '../constants/vehicleConstants';

/** Regex patterns matching backend @Pattern constraints. */
const VIN_PATTERN = /^[A-HJ-NPR-Z0-9]{17}$/;
const TEXT_PATTERN = /^[a-zA-Z0-9À-ÿ\s\-'.]+$/;
const LICENSE_PLATE_PATTERN = /^[A-Z0-9\-\s]{2,10}$/;
const DESCRIPTION_PATTERN = /^[a-zA-Z0-9À-ÿ\s.,!?()''+:\-/]*$/;
const LOCATION_PATTERN = /^[a-zA-Z0-9À-ÿ\s.,#\-'/]+$/;

/** Validates that availableUntil is in the future. */
export function validateAvailableUntil(availableUntil) {
  if (!availableUntil) return 'Available until date/time is required';
  if (new Date(availableUntil) <= new Date()) return 'Available until must be in the future';
  return null;
}

/** Validates VIN format and length. */
export function validateVin(vin) {
  if (!vin || vin.trim().length === 0) return 'VIN is required';
  if (vin.trim().length !== VIN_LENGTH) return `VIN must be exactly ${VIN_LENGTH} characters`;
  if (!VIN_PATTERN.test(vin.trim())) return 'VIN must contain only uppercase letters (except I, O, Q) and numbers';
  return null;
}

/** Validates make field. */
export function validateMake(make) {
  if (!make || !make.trim()) return 'Make is required';
  if (!TEXT_PATTERN.test(make.trim())) return 'Make can only contain letters, numbers, spaces, hyphens, and apostrophes';
  return null;
}

/** Validates model field. */
export function validateModel(model) {
  if (!model || !model.trim()) return 'Model is required';
  if (!TEXT_PATTERN.test(model.trim())) return 'Model can only contain letters, numbers, spaces, hyphens, and apostrophes';
  return null;
}

/** Validates the vehicle year against allowed range (max: next year for upcoming models). */
export function validateYear(year) {
  const currentYear = new Date().getFullYear();
  const minYear = currentYear - MAX_VEHICLE_AGE_YEARS;
  const maxYear = currentYear + 1;
  if (!year) return 'Year is required';
  if (year < minYear || year > maxYear) return `Year must be between ${minYear} and ${maxYear}`;
  return null;
}

/** Validates license plate format. */
export function validateLicensePlate(licensePlate) {
  if (!licensePlate || !licensePlate.trim()) return 'License plate is required';
  if (!LICENSE_PLATE_PATTERN.test(licensePlate.trim())) return 'License plate must be 2-10 uppercase alphanumeric characters';
  return null;
}

/** Validates the hourly rate. */
export function validateHourlyRate(rate) {
  if (!rate || rate < MIN_HOURLY_RATE) return `Hourly rate must be at least $${MIN_HOURLY_RATE.toFixed(2)}`;
  return null;
}

/** Validates description pattern (optional field). */
export function validateDescription(description) {
  if (description && description.trim() && !DESCRIPTION_PATTERN.test(description)) {
    return 'Description can only contain letters, numbers, spaces, and basic punctuation';
  }
  return null;
}

/** Validates general location pattern. */
export function validateGeneralLocation(location) {
  if (!location || !location.trim()) return 'General location is required';
  if (!LOCATION_PATTERN.test(location.trim())) return 'Location can only contain letters, numbers, spaces, commas, periods, hyphens, and #';
  return null;
}

/** Validates a single field by name. Returns error message or null. */
export function validateField(fieldName, value) {
  switch (fieldName) {
    case 'vin': return validateVin(value);
    case 'make': return validateMake(value);
    case 'model': return validateModel(value);
    case 'year': return validateYear(value ? Number(value) : null);
    case 'licensePlate': return validateLicensePlate(value);
    case 'category': return !value ? 'Category is required' : null;
    case 'fuelType': return !value ? 'Fuel type is required' : null;
    case 'hourlyRate': return validateHourlyRate(value ? Number(value) : null);
    case 'description': return validateDescription(value);
    case 'generalLocation': return validateGeneralLocation(value);
    case 'availableUntil': return validateAvailableUntil(value);
    default: return null;
  }
}

/** Validates all required vehicle form fields. */
export function validateVehicleForm(data) {
  const errors = {};
  const fields = ['vin', 'make', 'model', 'year', 'licensePlate', 'category', 'fuelType'];
  for (const field of fields) {
    const error = validateField(field, data[field]);
    if (error) errors[field] = error;
  }
  const descError = validateDescription(data.description);
  if (descError) errors.description = descError;
  return errors;
}

/** Validates the activate form fields (location, availableUntil, and hourlyRate). */
export function validateActivateForm(data) {
  const errors = {};
  const untilError = validateAvailableUntil(data.availableUntil);
  if (untilError) errors.availableUntil = untilError;
  const locError = validateGeneralLocation(data.generalLocation);
  if (locError) errors.generalLocation = locError;
  if (data.latitude === '' || data.latitude === null || data.latitude === undefined) {
    errors.latitude = 'Latitude is required';
  }
  if (data.longitude === '' || data.longitude === null || data.longitude === undefined) {
    errors.longitude = 'Longitude is required';
  }
  const rateError = validateHourlyRate(data.hourlyRate);
  if (rateError) errors.hourlyRate = rateError;
  return errors;
}
