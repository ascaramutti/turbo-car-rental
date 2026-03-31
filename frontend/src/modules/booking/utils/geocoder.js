let googleMapsLoaderPromise = null;

function getAddressComponent(result, type) {
  const match = result.address_components?.find((component) => component.types?.includes(type));
  return match?.long_name || '';
}

function formatAddress(result) {
  const streetNumber = getAddressComponent(result, 'street_number');
  const route = getAddressComponent(result, 'route');
  const city = getAddressComponent(result, 'locality') || getAddressComponent(result, 'administrative_area_level_2');

  const street = [streetNumber, route].filter(Boolean).join(' ').trim();
  const formatted = [street, city].filter(Boolean).join(', ').trim();

  return formatted || result.formatted_address;
}

export function loadGoogleMapsApi() {
  if (window.google?.maps) {
    return Promise.resolve(window.google);
  }

  if (googleMapsLoaderPromise) {
    return googleMapsLoaderPromise;
  }

  googleMapsLoaderPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[data-google-maps="true"]');

    const onLoad = () => {
      if (window.google?.maps) {
        resolve(window.google);
      } else {
        reject(new Error('Google Maps API loaded but unavailable'));
      }
    };

    const onError = () => reject(new Error('Failed to load Google Maps API'));

    if (existingScript) {
      existingScript.addEventListener('load', onLoad, { once: true });
      existingScript.addEventListener('error', onError, { once: true });
      return;
    }

    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
    if (!apiKey) {
      reject(new Error('Google Maps API key not configured'));
      return;
    }

    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places,geometry`;
    script.async = true;
    script.defer = true;
    script.dataset.googleMaps = 'true';
    script.addEventListener('load', onLoad, { once: true });
    script.addEventListener('error', onError, { once: true });
    document.head.appendChild(script);
  });

  return googleMapsLoaderPromise;
}

export async function geocodeAddress(address) {
  await loadGoogleMapsApi();

  const geocoder = new window.google.maps.Geocoder();

  return new Promise((resolve, reject) => {
    geocoder.geocode({ address }, (results, status) => {
      if (status === 'OK' && results && results.length > 0) {
        const { lat, lng } = results[0].geometry.location;
        resolve({
          latitude: lat(),
          longitude: lng(),
          formattedAddress: results[0].formatted_address,
        });
      } else {
        reject(new Error(`Geocoding failed: ${status}`));
      }
    });
  });
}

export async function reverseGeocode(latitude, longitude) {
  await loadGoogleMapsApi();

  const geocoder = new window.google.maps.Geocoder();

  return new Promise((resolve, reject) => {
    geocoder.geocode({ location: { lat: latitude, lng: longitude } }, (results, status) => {
      if (status === 'OK' && results && results.length > 0) {
        const bestResult =
          results.find((item) => getAddressComponent(item, 'route') || getAddressComponent(item, 'street_number'))
          || results[0];
        resolve(formatAddress(bestResult));
      } else {
        reject(new Error(`Reverse geocoding failed: ${status}`));
      }
    });
  });
}
