import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

let geocoderModule;

async function loadFreshGeocoder() {
  vi.resetModules();
  geocoderModule = await import('../utils/geocoder');
  return geocoderModule;
}

function makeFakeGeocoder(impl) {
  return function FakeGeocoder() {
    this.geocode = impl;
  };
}

function setupGoogleMaps(geocoderFactory) {
  window.google = { maps: { Geocoder: geocoderFactory } };
}

describe('geocoder utils', () => {
  beforeEach(() => {
    delete window.google;
    document.head.innerHTML = '';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ── loadGoogleMapsApi ────────────────────────────────────────────────

  describe('loadGoogleMapsApi', () => {
    it('resolves immediately if window.google.maps is already present', async () => {
      setupGoogleMaps(vi.fn());
      const { loadGoogleMapsApi } = await loadFreshGeocoder();
      await expect(loadGoogleMapsApi()).resolves.toBe(window.google);
    });

    it('rejects if no API key is configured and no script exists', async () => {
      const original = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
      import.meta.env.VITE_GOOGLE_MAPS_API_KEY = '';
      const { loadGoogleMapsApi } = await loadFreshGeocoder();
      await expect(loadGoogleMapsApi()).rejects.toThrow('Google Maps API key not configured');
      import.meta.env.VITE_GOOGLE_MAPS_API_KEY = original;
    });

    it('appends a script tag when an API key is configured', async () => {
      import.meta.env.VITE_GOOGLE_MAPS_API_KEY = 'fake-key';
      const { loadGoogleMapsApi } = await loadFreshGeocoder();
      const promise = loadGoogleMapsApi();
      const script = document.head.querySelector('script[data-google-maps="true"]');
      expect(script).not.toBeNull();
      expect(script.src).toContain('fake-key');

      // Simulate load failure path so the promise settles.
      script.dispatchEvent(new Event('error'));
      await expect(promise).rejects.toThrow('Failed to load Google Maps API');
    });

    it('resolves when the appended script loads and google is available', async () => {
      import.meta.env.VITE_GOOGLE_MAPS_API_KEY = 'fake-key';
      const { loadGoogleMapsApi } = await loadFreshGeocoder();
      const promise = loadGoogleMapsApi();
      const script = document.head.querySelector('script[data-google-maps="true"]');
      setupGoogleMaps(vi.fn());
      script.dispatchEvent(new Event('load'));
      await expect(promise).resolves.toBe(window.google);
    });

    it('rejects when the script loads but google is missing', async () => {
      import.meta.env.VITE_GOOGLE_MAPS_API_KEY = 'fake-key';
      const { loadGoogleMapsApi } = await loadFreshGeocoder();
      const promise = loadGoogleMapsApi();
      const script = document.head.querySelector('script[data-google-maps="true"]');
      script.dispatchEvent(new Event('load'));
      await expect(promise).rejects.toThrow('Google Maps API loaded but unavailable');
    });
  });

  // ── geocodeAddress ───────────────────────────────────────────────────

  describe('geocodeAddress', () => {
    it('returns coordinates and formatted address on OK status', async () => {
      const geocode = vi.fn((_, cb) =>
        cb(
          [
            {
              geometry: { location: { lat: () => 49.28, lng: () => -123.12 } },
              formatted_address: '123 Main St, Vancouver',
            },
          ],
          'OK'
        )
      );
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { geocodeAddress } = await loadFreshGeocoder();

      const result = await geocodeAddress('123 Main St');
      expect(result).toEqual({
        latitude: 49.28,
        longitude: -123.12,
        formattedAddress: '123 Main St, Vancouver',
      });
    });

    it('rejects when the geocoder reports a non-OK status', async () => {
      const geocode = vi.fn((_, cb) => cb([], 'ZERO_RESULTS'));
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { geocodeAddress } = await loadFreshGeocoder();
      await expect(geocodeAddress('nowhere')).rejects.toThrow('Geocoding failed: ZERO_RESULTS');
    });
  });

  // ── reverseGeocode ───────────────────────────────────────────────────

  describe('reverseGeocode', () => {
    it('formats street + city from address components when present', async () => {
      const geocode = vi.fn((_, cb) =>
        cb(
          [
            {
              address_components: [
                { long_name: '123', types: ['street_number'] },
                { long_name: 'Granville St', types: ['route'] },
                { long_name: 'Vancouver', types: ['locality'] },
              ],
              formatted_address: 'fallback',
            },
          ],
          'OK'
        )
      );
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { reverseGeocode } = await loadFreshGeocoder();
      await expect(reverseGeocode(49, -123)).resolves.toBe('123 Granville St, Vancouver');
    });

    it('falls back to administrative_area_level_2 when locality is missing', async () => {
      const geocode = vi.fn((_, cb) =>
        cb(
          [
            {
              address_components: [
                { long_name: 'Pine Rd', types: ['route'] },
                { long_name: 'Metro Region', types: ['administrative_area_level_2'] },
              ],
              formatted_address: 'fallback',
            },
          ],
          'OK'
        )
      );
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { reverseGeocode } = await loadFreshGeocoder();
      await expect(reverseGeocode(49, -123)).resolves.toBe('Pine Rd, Metro Region');
    });

    it('returns formatted_address when no usable components are present', async () => {
      const geocode = vi.fn((_, cb) =>
        cb(
          [
            {
              address_components: [],
              formatted_address: 'Just a fallback string',
            },
          ],
          'OK'
        )
      );
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { reverseGeocode } = await loadFreshGeocoder();
      await expect(reverseGeocode(0, 0)).resolves.toBe('Just a fallback string');
    });

    it('rejects when the geocoder reports a non-OK status', async () => {
      const geocode = vi.fn((_, cb) => cb(null, 'ERROR'));
      setupGoogleMaps(makeFakeGeocoder(geocode));
      const { reverseGeocode } = await loadFreshGeocoder();
      await expect(reverseGeocode(0, 0)).rejects.toThrow('Reverse geocoding failed: ERROR');
    });
  });
});
