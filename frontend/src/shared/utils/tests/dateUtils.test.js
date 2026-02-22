import { describe, it, expect } from 'vitest';
import { formatDateTime } from '../dateUtils';

describe('formatDateTime', () => {
  it('returns empty string for null', () => {
    expect(formatDateTime(null)).toBe('');
  });

  it('returns empty string for undefined', () => {
    expect(formatDateTime(undefined)).toBe('');
  });

  it('returns empty string for empty input', () => {
    expect(formatDateTime('')).toBe('');
  });

  it('formats a valid ISO date string into the local datetime representation', () => {
    const iso = '2026-04-07T15:30:00Z';
    const expected = new Date(iso).toLocaleString();
    expect(formatDateTime(iso)).toBe(expected);
  });

  it('returns a non-empty string for a valid date', () => {
    const result = formatDateTime('2026-01-01T00:00:00Z');
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });
});
