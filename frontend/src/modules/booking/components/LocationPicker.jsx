import { useEffect, useRef, useState } from 'react';
import { MapPin, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import { geocodeAddress, loadGoogleMapsApi } from '../utils/geocoder.js';

const DEFAULT_CENTER = { lat: -34.397, lng: 150.644 }; // Sydney, Australia
const DEFAULT_ZOOM = 13;

function toNumber(value) {
  if (value === '' || value == null) return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * LocationPicker - Interactive Google Map for location selection
 * Allows clicking on map or searching by address
 */
export default function LocationPicker({
  onLocationSelect,
  initialLat,
  initialLon,
  markers = [],
  onMarkerSelect,
  showSearch = true,
  helperText = 'Drag the marker or click on the map to select a location.',
  addressPlaceholder = "Search address... (e.g., 'Times Square, New York')",
  heightClassName = 'h-96',
}) {
  const mapContainer = useRef(null);
  const map = useRef(null);
  const marker = useRef(null);
  const vehicleMarkers = useRef([]);
  const onLocationSelectRef = useRef(onLocationSelect);
  const onMarkerSelectRef = useRef(onMarkerSelect);
  const [mapLoaded, setMapLoaded] = useState(false);
  const [mapReady, setMapReady] = useState(false);
  const [addressInput, setAddressInput] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  useEffect(() => {
    onLocationSelectRef.current = onLocationSelect;
  }, [onLocationSelect]);

  useEffect(() => {
    onMarkerSelectRef.current = onMarkerSelect;
  }, [onMarkerSelect]);

  // Check if Google Maps API is loaded
  useEffect(() => {
    let isMounted = true;

    loadGoogleMapsApi()
      .then(() => {
        if (isMounted) {
          setMapLoaded(true);
        }
      })
      .catch((error) => {
        toast.error(error.message || 'Google Maps could not be loaded');
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Initialize map
  useEffect(() => {
    if (!mapLoaded || !mapContainer.current || map.current) return;

    const initialLatitude = toNumber(initialLat);
    const initialLongitude = toNumber(initialLon);
    const initialCenter = {
      lat: initialLatitude ?? DEFAULT_CENTER.lat,
      lng: initialLongitude ?? DEFAULT_CENTER.lng,
    };

    map.current = new window.google.maps.Map(mapContainer.current, {
      zoom: DEFAULT_ZOOM,
      center: initialCenter,
      mapTypeControl: true,
      fullscreenControl: true,
      streetViewControl: false,
    });

    if (typeof onLocationSelectRef.current === 'function') {
      marker.current = new window.google.maps.Marker({
        position: initialCenter,
        map: map.current,
        draggable: true,
        title: 'Selected location',
      });

      marker.current.addListener('dragend', () => {
        const pos = marker.current?.getPosition();
        if (!pos) return;
        onLocationSelectRef.current?.(pos.lat(), pos.lng());
      });

      map.current.addListener('click', (e) => {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        marker.current?.setPosition({ lat, lng });
        onLocationSelectRef.current?.(lat, lng);
      });
    }

    setMapReady(true);
  }, [mapLoaded, initialLat, initialLon]);

  useEffect(() => {
    if (!mapReady || !map.current || !marker.current) return;

    const latitude = toNumber(initialLat);
    const longitude = toNumber(initialLon);
    if (latitude == null || longitude == null) return;

    const nextPosition = { lat: latitude, lng: longitude };
    marker.current.setPosition(nextPosition);
    map.current.setCenter(nextPosition);
  }, [mapReady, initialLat, initialLon]);

  useEffect(() => {
    if (!mapReady || !map.current) return;

    vehicleMarkers.current.forEach((existingMarker) => existingMarker.setMap(null));
    vehicleMarkers.current = [];

    const validMarkers = markers.filter((item) => toNumber(item.latitude) != null && toNumber(item.longitude) != null);

    validMarkers.forEach((item) => {
      const vehicleMarker = new window.google.maps.Marker({
        position: { lat: Number(item.latitude), lng: Number(item.longitude) },
        map: map.current,
        title: item.title || item.label || 'Vehicle location',
      });

      if (typeof onMarkerSelectRef.current === 'function') {
        vehicleMarker.addListener('click', () => {
          onMarkerSelectRef.current?.(item);
        });
      }

      vehicleMarkers.current.push(vehicleMarker);
    });

    if (validMarkers.length === 0) return;

    const bounds = new window.google.maps.LatLngBounds();
    validMarkers.forEach((item) => {
      bounds.extend({ lat: Number(item.latitude), lng: Number(item.longitude) });
    });

    const selectedPosition = marker.current?.getPosition();
    if (selectedPosition) {
      bounds.extend(selectedPosition);
    }

    map.current.fitBounds(bounds);
  }, [mapReady, markers]);

  const handleSearchAddress = async (e) => {
    e.preventDefault();

    if (!mapReady || !map.current) {
      toast.error('Map is still loading, please try again in a moment.');
      return;
    }

    if (!addressInput.trim()) {
      toast.error('Please enter an address');
      return;
    }

    setIsSearching(true);
    try {
      const result = await geocodeAddress(addressInput);
      const { latitude, longitude, formattedAddress } = result;

      map.current.setCenter({ lat: latitude, lng: longitude });
      map.current.setZoom(Math.max(map.current.getZoom() ?? DEFAULT_ZOOM, 14));

      if (marker.current) {
        marker.current.setPosition({ lat: latitude, lng: longitude });
      }

      onLocationSelectRef.current?.(latitude, longitude);
      toast.success(`Location found: ${formattedAddress}`);
      setAddressInput('');
    } catch (error) {
      toast.error(error.message);
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="w-full">
      {showSearch && (
        <form onSubmit={handleSearchAddress} className="mb-4 flex gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              value={addressInput}
              onChange={(e) => setAddressInput(e.target.value)}
              placeholder={addressPlaceholder}
              className="w-full px-4 py-2 pl-10 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500"
              disabled={isSearching || !mapReady}
            />
            <MapPin className="absolute left-3 top-2.5 w-5 h-5 text-gray-400" />
          </div>
          <button
            type="submit"
            disabled={isSearching || !mapReady}
            className="px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 disabled:bg-gray-400 flex items-center gap-2"
          >
            <Search className="w-4 h-4" />
            {isSearching ? 'Searching...' : 'Search'}
          </button>
        </form>
      )}

      <div
        ref={mapContainer}
        className={`w-full ${heightClassName} rounded-lg border border-gray-300 shadow-md`}
      />

      {helperText && <p className="mt-2 text-sm text-gray-600">{helperText}</p>}
    </div>
  );
}
