import { describe, it, expect, vi } from 'vitest';
import {
  validateAvailableUntil,
  validateVin,
  validateYear,
  validateHourlyRate,
  validateVehicleForm,
  validateActivateForm,
} from '../utils/vehicleValidation';

describe('validateAvailableUntil', () => {
  it('returns null for a valid future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 1);
    const result = validateAvailableUntil(future.toISOString());
    expect(result).toBeNull();
  });

  it('returns error for null', () => {
    const result = validateAvailableUntil(null);
    expect(result).toBe('Available until date/time is required');
  });

  it('returns error for empty string', () => {
    const result = validateAvailableUntil('');
    expect(result).toBe('Available until date/time is required');
  });

  it('returns error for a past date', () => {
    const past = new Date('2020-01-01T00:00:00');
    const result = validateAvailableUntil(past.toISOString());
    expect(result).toBe('Available until must be in the future');
  });

  it('returns error for current time (not strictly future)', () => {
    // Use a date slightly in the past to guarantee it is <= now
    const now = new Date();
    now.setSeconds(now.getSeconds() - 1);
    const result = validateAvailableUntil(now.toISOString());
    expect(result).toBe('Available until must be in the future');
  });
});

describe('validateVin', () => {
  it('returns null for a valid 17-character VIN', () => {
    const result = validateVin('WVWZZZ3CZWE123456');
    expect(result).toBeNull();
  });

  it('returns error for a VIN that is too short', () => {
    const result = validateVin('WVWZZZ3CZ');
    expect(result).toBe('VIN must be exactly 17 characters');
  });

  it('returns error for a VIN that is too long', () => {
    const result = validateVin('WVWZZZ3CZWE12345678');
    expect(result).toBe('VIN must be exactly 17 characters');
  });

  it('returns error for empty VIN', () => {
    const result = validateVin('');
    expect(result).toBe('VIN is required');
  });

  it('returns error for null VIN', () => {
    const result = validateVin(null);
    expect(result).toBe('VIN is required');
  });
});

describe('validateYear', () => {
  const currentYear = new Date().getFullYear();
  const minYear = currentYear - 20;

  it('returns null for a valid year within range', () => {
    const result = validateYear(currentYear);
    expect(result).toBeNull();
  });

  it('returns null for the minimum allowed year', () => {
    const result = validateYear(minYear);
    expect(result).toBeNull();
  });

  it('returns error for a year that is too old (> 20 years)', () => {
    const result = validateYear(minYear - 1);
    expect(result).toBe(`Year must be between ${minYear} and ${currentYear + 1}`);
  });

  it('returns null for next year (upcoming models)', () => {
    const result = validateYear(currentYear + 1);
    expect(result).toBeNull();
  });

  it('returns error for a year too far in the future', () => {
    const result = validateYear(currentYear + 2);
    expect(result).toBe(`Year must be between ${minYear} and ${currentYear + 1}`);
  });

  it('returns error for missing year', () => {
    const result = validateYear(null);
    expect(result).toBe('Year is required');
  });
});

describe('validateHourlyRate', () => {
  it('returns null for a valid hourly rate', () => {
    const result = validateHourlyRate(25.0);
    expect(result).toBeNull();
  });

  it('returns null for the minimum hourly rate (1.00)', () => {
    const result = validateHourlyRate(1.0);
    expect(result).toBeNull();
  });

  it('returns error for rate below minimum', () => {
    const result = validateHourlyRate(0.5);
    expect(result).toBe('Hourly rate must be at least $1.00');
  });

  it('returns error for zero rate', () => {
    const result = validateHourlyRate(0);
    expect(result).toBe('Hourly rate must be at least $1.00');
  });

  it('returns error for null rate', () => {
    const result = validateHourlyRate(null);
    expect(result).toBe('Hourly rate must be at least $1.00');
  });
});

describe('validateVehicleForm', () => {
  const VALID_FORM = {
    vin: 'WVWZZZ3CZWE123456',
    make: 'Volkswagen',
    model: 'Jetta',
    year: 2022,
    licensePlate: 'ABC 123',
    category: 'SEDAN',
    fuelType: 'GASOLINE',
  };

  it('returns empty object for a fully valid form', () => {
    const errors = validateVehicleForm(VALID_FORM);
    expect(Object.keys(errors)).toHaveLength(0);
  });

  it('returns errors for missing required fields', () => {
    const errors = validateVehicleForm({
      vin: '',
      make: '',
      model: '',
      year: null,
      licensePlate: '',
      category: '',
      fuelType: '',
    });

    expect(errors.vin).toBeDefined();
    expect(errors.make).toBe('Make is required');
    expect(errors.model).toBe('Model is required');
    expect(errors.year).toBeDefined();
    expect(errors.licensePlate).toBe('License plate is required');
    expect(errors.category).toBe('Category is required');
    expect(errors.fuelType).toBe('Fuel type is required');
  });

  it('does not validate hourlyRate (moved to activate form)', () => {
    const errors = validateVehicleForm(VALID_FORM);
    expect(errors.hourlyRate).toBeUndefined();
  });

  it('returns only vin error when only vin is invalid', () => {
    const errors = validateVehicleForm({ ...VALID_FORM, vin: 'SHORT' });
    expect(Object.keys(errors)).toHaveLength(1);
    expect(errors.vin).toBe('VIN must be exactly 17 characters');
  });
});

describe('validateActivateForm', () => {
  it('returns empty object for valid activate data', () => {
    const future = new Date();
    future.setDate(future.getDate() + 1);
    const errors = validateActivateForm({
      availableUntil: future.toISOString(),
      generalLocation: 'Vancouver',
      latitude: 49.2827,
      longitude: -123.1207,
      hourlyRate: 25.0,
    });
    expect(Object.keys(errors)).toHaveLength(0);
  });

  it('returns errors for missing activate fields', () => {
    const errors = validateActivateForm({
      availableUntil: '',
      generalLocation: '',
      latitude: null,
      longitude: null,
      hourlyRate: null,
    });
    expect(errors.availableUntil).toBeDefined();
    expect(errors.generalLocation).toBe('General location is required');
    expect(errors.latitude).toBe('Latitude is required');
    expect(errors.longitude).toBe('Longitude is required');
    expect(errors.hourlyRate).toBeDefined();
  });

  it('returns hourlyRate error when rate is below minimum', () => {
    const future = new Date();
    future.setDate(future.getDate() + 1);
    const errors = validateActivateForm({
      availableUntil: future.toISOString(),
      generalLocation: 'Vancouver',
      latitude: 49.2827,
      longitude: -123.1207,
      hourlyRate: 0.5,
    });
    expect(errors.hourlyRate).toBe('Hourly rate must be at least $1.00');
  });
});
