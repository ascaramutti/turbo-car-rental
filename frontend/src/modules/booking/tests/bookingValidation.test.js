import { describe, it, expect } from 'vitest';
import {
  validateStartTime,
  validateEndTime,
  validateReason,
  validateBookingField,
  validateCreateBookingForm,
} from '../utils/bookingValidation';

// ── validateStartTime ─────────────────────────────────────────────────────

describe('validateStartTime', () => {
  it('returns null for a valid future date', () => {
    const future = new Date();
    future.setHours(future.getHours() + 2);
    expect(validateStartTime(future.toISOString())).toBeNull();
  });

  it('returns error for a past date', () => {
    const past = new Date('2020-01-01T00:00:00');
    expect(validateStartTime(past.toISOString())).toBe('Start time must be in the future');
  });

  it('returns error for current time (not strictly future)', () => {
    const now = new Date();
    now.setSeconds(now.getSeconds() - 1);
    expect(validateStartTime(now.toISOString())).toBe('Start time must be in the future');
  });

  it('returns error for null', () => {
    expect(validateStartTime(null)).toBe('Start time is required');
  });

  it('returns error for empty string', () => {
    expect(validateStartTime('')).toBe('Start time is required');
  });
});

// ── validateEndTime ───────────────────────────────────────────────────────

describe('validateEndTime', () => {
  const makeTime = (hoursFromNow) => {
    const d = new Date();
    d.setHours(d.getHours() + hoursFromNow);
    return d.toISOString();
  };

  it('returns null for a valid end time (>= start + 4h)', () => {
    const start = makeTime(2);
    const end = makeTime(7); // 5h after now, 5h gap
    expect(validateEndTime(end, start)).toBeNull();
  });

  it('returns null for exactly the minimum gap (4h)', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-01T12:00:00').toISOString();
    expect(validateEndTime(end, start)).toBeNull();
  });

  it('returns null for exactly the maximum gap (24h)', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-02T08:00:00').toISOString();
    expect(validateEndTime(end, start)).toBeNull();
  });

  it('returns error for gap shorter than 4 hours', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-01T10:00:00').toISOString(); // 2h gap
    expect(validateEndTime(end, start)).toBe('Minimum booking duration is 4 hours');
  });

  it('returns error for gap longer than 24 hours', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-03T08:00:00').toISOString(); // 48h gap
    expect(validateEndTime(end, start)).toBe('Maximum booking duration is 24 hours');
  });

  it('returns error when end time is before start time', () => {
    const start = new Date('2030-06-01T12:00:00').toISOString();
    const end = new Date('2030-06-01T08:00:00').toISOString();
    expect(validateEndTime(end, start)).toBe('End time must be after start time');
  });

  it('returns error when end time equals start time', () => {
    const time = new Date('2030-06-01T12:00:00').toISOString();
    expect(validateEndTime(time, time)).toBe('End time must be after start time');
  });

  it('returns error for null end time', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    expect(validateEndTime(null, start)).toBe('End time is required');
  });

  it('returns error for empty end time', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    expect(validateEndTime('', start)).toBe('End time is required');
  });

  it('returns null when end time is provided but startTime is missing', () => {
    const end = new Date('2030-06-01T12:00:00').toISOString();
    // No cross-field validation possible without start time
    expect(validateEndTime(end, null)).toBeNull();
  });
});

// ── validateReason ────────────────────────────────────────────────────────

describe('validateReason', () => {
  it('returns null for a valid reason', () => {
    expect(validateReason('Vehicle has a flat tire.')).toBeNull();
  });

  it('returns null for a reason with allowed punctuation', () => {
    expect(validateReason('Cannot make it, sorry!')).toBeNull();
  });

  it('returns error for empty string', () => {
    expect(validateReason('')).toBe('Reason is required');
  });

  it('returns error for null', () => {
    expect(validateReason(null)).toBe('Reason is required');
  });

  it('returns error for whitespace-only string', () => {
    expect(validateReason('   ')).toBe('Reason is required');
  });

  it('returns error when reason exceeds 500 characters', () => {
    const longReason = 'a'.repeat(501);
    expect(validateReason(longReason)).toBe('Reason cannot exceed 500 characters');
  });

  it('returns null for a reason of exactly 500 characters', () => {
    const maxReason = 'a'.repeat(500);
    expect(validateReason(maxReason)).toBeNull();
  });

  it('returns error for reason containing SQL injection characters (semicolon)', () => {
    expect(validateReason("reason; DROP TABLE bookings;")).toBe(
      'Reason can only contain letters, numbers, spaces, and basic punctuation'
    );
  });

  it('returns error for reason containing angle brackets (XSS)', () => {
    expect(validateReason('<script>alert(1)</script>')).toBe(
      'Reason can only contain letters, numbers, spaces, and basic punctuation'
    );
  });
});

// ── validateBookingField ──────────────────────────────────────────────────

describe('validateBookingField', () => {
  it('routes "startTime" to validateStartTime and returns error for past date', () => {
    const past = new Date('2020-01-01').toISOString();
    expect(validateBookingField('startTime', past)).toBe('Start time must be in the future');
  });

  it('routes "startTime" to validateStartTime and returns null for future date', () => {
    const future = new Date();
    future.setHours(future.getHours() + 3);
    expect(validateBookingField('startTime', future.toISOString())).toBeNull();
  });

  it('routes "endTime" to validateEndTime and uses formData.startTime for cross-field validation', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-01T10:00:00').toISOString(); // only 2h gap
    expect(validateBookingField('endTime', end, { startTime: start })).toBe(
      'Minimum booking duration is 4 hours'
    );
  });

  it('routes "endTime" and returns null for valid end time', () => {
    const start = new Date('2030-06-01T08:00:00').toISOString();
    const end = new Date('2030-06-01T13:00:00').toISOString(); // 5h gap
    expect(validateBookingField('endTime', end, { startTime: start })).toBeNull();
  });

  it('routes "reason" to validateReason and returns error for empty', () => {
    expect(validateBookingField('reason', '')).toBe('Reason is required');
  });

  it('routes "reason" to validateReason and returns null for valid reason', () => {
    expect(validateBookingField('reason', 'A valid reason.')).toBeNull();
  });

  it('returns error for "vehicleId" when value is null', () => {
    expect(validateBookingField('vehicleId', null)).toBe('Vehicle ID is required');
  });

  it('returns null for "vehicleId" when value is provided', () => {
    expect(validateBookingField('vehicleId', 42)).toBeNull();
  });

  it('returns null for unknown field names', () => {
    expect(validateBookingField('unknownField', 'anything')).toBeNull();
  });
});

// ── validateCreateBookingForm ─────────────────────────────────────────────

describe('validateCreateBookingForm', () => {
  const FUTURE_START = new Date('2030-06-01T08:00:00').toISOString();
  const VALID_END = new Date('2030-06-01T13:00:00').toISOString(); // 5h gap

  it('returns empty object for a fully valid form', () => {
    const errors = validateCreateBookingForm({
      vehicleId: 10,
      startTime: FUTURE_START,
      endTime: VALID_END,
    });
    expect(Object.keys(errors)).toHaveLength(0);
  });

  it('returns error for missing vehicleId', () => {
    const errors = validateCreateBookingForm({
      vehicleId: null,
      startTime: FUTURE_START,
      endTime: VALID_END,
    });
    expect(errors.vehicleId).toBe('Vehicle ID is required');
  });

  it('returns error for missing startTime', () => {
    const errors = validateCreateBookingForm({
      vehicleId: 10,
      startTime: '',
      endTime: VALID_END,
    });
    expect(errors.startTime).toBe('Start time is required');
  });

  it('returns error for past startTime', () => {
    const errors = validateCreateBookingForm({
      vehicleId: 10,
      startTime: '2020-01-01T00:00:00',
      endTime: VALID_END,
    });
    expect(errors.startTime).toBe('Start time must be in the future');
  });

  it('returns error for missing endTime', () => {
    const errors = validateCreateBookingForm({
      vehicleId: 10,
      startTime: FUTURE_START,
      endTime: '',
    });
    expect(errors.endTime).toBe('End time is required');
  });

  it('returns error for endTime too close to startTime', () => {
    const tooSoon = new Date('2030-06-01T10:00:00').toISOString(); // only 2h gap
    const errors = validateCreateBookingForm({
      vehicleId: 10,
      startTime: FUTURE_START,
      endTime: tooSoon,
    });
    expect(errors.endTime).toBe('Minimum booking duration is 4 hours');
  });

  it('returns multiple errors for all fields missing', () => {
    const errors = validateCreateBookingForm({
      vehicleId: null,
      startTime: '',
      endTime: '',
    });
    expect(errors.vehicleId).toBeDefined();
    expect(errors.startTime).toBeDefined();
    expect(errors.endTime).toBeDefined();
  });
});
