import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

/**
 * Provide a fake Stripe publishable key so PaymentModal initialises stripePromise
 * during tests. The actual Stripe SDK is mocked at the module level inside test files.
 */
vi.stubEnv('VITE_STRIPE_PUBLISHABLE_KEY', 'pk_test_fake_for_unit_tests');

/**
 * Mock localStorage for jsdom environment.
 * AuthContext reads from localStorage on mount.
 */
const localStorageMock = (() => {
  let store = {};
  return {
    getItem: (key) => store[key] ?? null,
    setItem: (key, value) => { store[key] = String(value); },
    removeItem: (key) => { delete store[key]; },
    clear: () => { store = {}; },
  };
})();

Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock });
