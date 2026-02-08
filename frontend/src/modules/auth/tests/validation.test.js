import { describe, it, expect } from 'vitest';
import {
  validateField,
  validateDateOfBirth,
  validatePasswordMatch,
  extractErrorMessage,
} from '../utils/validation';

describe('validateField', () => {
  describe('email', () => {
    it('returns error for empty email', () => {
      expect(validateField('email', '')).toBe('Email is required');
    });

    it('returns error for invalid email format', () => {
      expect(validateField('email', 'not-an-email')).toBe('Invalid email format');
      expect(validateField('email', 'john@')).toBe('Invalid email format');
      expect(validateField('email', '@example.com')).toBe('Invalid email format');
    });

    it('returns null for valid email', () => {
      expect(validateField('email', 'john@example.com')).toBeNull();
      expect(validateField('email', 'user.name+tag@domain.co')).toBeNull();
    });
  });

  describe('password', () => {
    it('returns error for empty password', () => {
      expect(validateField('password', '')).toBe('Password is required');
    });

    it('returns error for short password', () => {
      expect(validateField('password', '12345')).toBe('Password must be between 6 and 100 characters');
    });

    it('returns null for valid password', () => {
      expect(validateField('password', 'secure123')).toBeNull();
      expect(validateField('password', '123456')).toBeNull();
    });
  });

  describe('firstName', () => {
    it('returns error for empty first name', () => {
      expect(validateField('firstName', '')).toBe('First name is required');
    });

    it('returns error for name with numbers', () => {
      expect(validateField('firstName', 'John123')).toBe(
        'Only letters, accents, apostrophes, and hyphens allowed'
      );
    });

    it('returns error for name with special characters', () => {
      expect(validateField('firstName', '<script>')).toBeTruthy();
    });

    it('returns null for valid names with accents and hyphens', () => {
      expect(validateField('firstName', 'María')).toBeNull();
      expect(validateField('firstName', "O'Brien")).toBeNull();
      expect(validateField('firstName', 'Jean-Pierre')).toBeNull();
    });
  });

  describe('lastName', () => {
    it('returns error for empty last name', () => {
      expect(validateField('lastName', '')).toBe('Last name is required');
    });

    it('returns null for valid last name', () => {
      expect(validateField('lastName', 'García')).toBeNull();
      expect(validateField('lastName', "O'Connor")).toBeNull();
    });
  });

  describe('phoneNumber (optional)', () => {
    it('returns null for empty phone (optional field)', () => {
      expect(validateField('phoneNumber', '')).toBeNull();
    });

    it('returns null for valid phone formats', () => {
      expect(validateField('phoneNumber', '+1 604 555 0001')).toBeNull();
      expect(validateField('phoneNumber', '(604) 555-0001')).toBeNull();
      expect(validateField('phoneNumber', '6045550001')).toBeNull();
    });

    it('returns error for invalid phone', () => {
      expect(validateField('phoneNumber', 'phone')).toBeTruthy();
      expect(validateField('phoneNumber', '+1')).toBeTruthy();
    });
  });

  describe('streetAddress', () => {
    it('returns error for empty street address', () => {
      expect(validateField('streetAddress', '')).toBe('Street address is required');
    });

    it('returns error for SQL injection characters', () => {
      expect(validateField('streetAddress', "'; DROP TABLE;")).toBeTruthy();
    });

    it('returns null for valid street addresses', () => {
      expect(validateField('streetAddress', '456 Granville St')).toBeNull();
      expect(validateField('streetAddress', '100-B King Rd. #5')).toBeNull();
    });
  });

  describe('city', () => {
    it('returns error for empty city', () => {
      expect(validateField('city', '')).toBe('City is required');
    });

    it('returns error for city with numbers', () => {
      expect(validateField('city', 'Vancouver123')).toBeTruthy();
    });

    it('returns null for valid cities', () => {
      expect(validateField('city', 'Vancouver')).toBeNull();
      expect(validateField('city', 'North Vancouver')).toBeNull();
    });
  });

  describe('postalCode', () => {
    it('returns error for empty postal code', () => {
      expect(validateField('postalCode', '')).toBe('Postal code is required');
    });

    it('returns error for US zip format', () => {
      expect(validateField('postalCode', '90210')).toBe('Invalid postal code format (e.g. V6B 1A1)');
    });

    it('returns null for valid Canadian postal codes', () => {
      expect(validateField('postalCode', 'V6C 1T2')).toBeNull();
      expect(validateField('postalCode', 'V6C1T2')).toBeNull();
    });
  });

  describe('otp', () => {
    it('returns error for empty otp', () => {
      expect(validateField('otp', '')).toBe('OTP code is required');
    });

    it('returns error for non-6-digit otp', () => {
      expect(validateField('otp', '12345')).toBe('OTP must be exactly 6 digits');
      expect(validateField('otp', 'abcdef')).toBe('OTP must be exactly 6 digits');
    });

    it('returns null for valid 6-digit otp', () => {
      expect(validateField('otp', '123456')).toBeNull();
      expect(validateField('otp', '000001')).toBeNull();
    });
  });
});

describe('validateDateOfBirth', () => {
  it('returns error for empty date', () => {
    expect(validateDateOfBirth('')).toBe('Date of birth is required');
    expect(validateDateOfBirth(null)).toBe('Date of birth is required');
  });

  it('returns error for future date', () => {
    expect(validateDateOfBirth('2099-01-01')).toBe('Date of birth must be in the past');
  });

  it('returns null for valid past date', () => {
    expect(validateDateOfBirth('1995-06-15')).toBeNull();
    expect(validateDateOfBirth('2000-01-01')).toBeNull();
  });
});

describe('validatePasswordMatch', () => {
  it('returns error for empty confirm password', () => {
    expect(validatePasswordMatch('pass123', '')).toBe('Please confirm your password');
  });

  it('returns error when passwords do not match', () => {
    expect(validatePasswordMatch('pass123', 'pass456')).toBe('Passwords do not match');
  });

  it('returns null when passwords match', () => {
    expect(validatePasswordMatch('secure123', 'secure123')).toBeNull();
  });
});

describe('extractErrorMessage', () => {
  it('returns generic message when no response data', () => {
    expect(extractErrorMessage({})).toBe('Something went wrong. Please try again.');
    expect(extractErrorMessage({ response: {} })).toBe('Something went wrong. Please try again.');
  });

  it('extracts field-level messages from VALIDATION-001', () => {
    const error = {
      response: {
        data: {
          code: 'VALIDATION-001',
          details: {
            email: 'must not be blank',
            password: 'size must be at least 6 characters',
          },
        },
      },
    };
    expect(extractErrorMessage(error)).toBe('must not be blank. size must be at least 6 characters');
  });

  it('extracts message from business errors', () => {
    const error = {
      response: {
        data: {
          code: 'AUTH-001',
          message: 'Email already registered',
        },
      },
    };
    expect(extractErrorMessage(error)).toBe('Email already registered');
  });
});
