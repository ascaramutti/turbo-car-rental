import { useState, useCallback } from 'react';
import { Loader2, Car, MapPin, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import { searchVehicles } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import VehicleSearchCard from '../components/VehicleSearchCard';
import VehicleDetailModal from '../components/VehicleDetailModal';

/** Service type filter options. */
const SERVICE_TYPE_OPTIONS = [
  { value: 'TAXI_AND_DELIVERY', label: 'Taxi & Delivery' },
  { value: 'DELIVERY_ONLY', label: 'Delivery Only' },
];

/** Sort options for the results listing. */
const SORT_OPTIONS = [
  { key: 'closest', label: 'Closest' },
  { key: 'cheapest', label: 'Cheapest' },
  { key: 'best_rated', label: 'Best Rated' },
];

/** Empty-state message when search returns no vehicles. */
const EMPTY_STATE_MESSAGE = 'No vehicles found. Try adjusting your filters.';

/** Validation limits for search filters. */
const VALIDATION = {
  MIN_LATITUDE: -90,
  MAX_LATITUDE: 90,
  MIN_LONGITUDE: -180,
  MAX_LONGITUDE: 180,
  MIN_RADIUS_KM: 0.1,
  MAX_RADIUS_KM: 100,
  MIN_PRICE: 0,
  MAX_PRICE: 10000,
};

/** Default filter state. */
const DEFAULT_FILTERS = {
  latitude: '',
  longitude: '',
  radiusKm: '10',
  startTime: '',
  endTime: '',
  serviceType: '',
  fuelType: '',
  minPrice: '',
  maxPrice: '',
};

/**
 * Sorts vehicle results according to the chosen sort key.
 * @param {Object[]} vehicles
 * @param {string} sortKey
 * @returns {Object[]}
 */
function sortVehicles(vehicles, sortKey) {
  const copy = [...vehicles];
  if (sortKey === 'cheapest') {
    return copy.sort((a, b) => Number(a.hourlyRate) - Number(b.hourlyRate));
  }
  if (sortKey === 'best_rated') {
    return copy.sort((a, b) => Number(b.ownerRating ?? 0) - Number(a.ownerRating ?? 0));
  }
  // 'closest' — keep original API order (already sorted by distance)
  return copy;
}

/**
 * Driver dashboard and vehicle search page.
 * Layout: filter sidebar (left) | available listings (center) | map placeholder (right).
 */
export default function DriverSearchPage() {
  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [sortKey, setSortKey] = useState('closest');
  const [selectedVehicle, setSelectedVehicle] = useState(null);

  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [serviceTypeChecks, setServiceTypeChecks] = useState({
    TAXI_AND_DELIVERY: false,
    DELIVERY_ONLY: false,
  });
  const [ownerRatingFilter, setOwnerRatingFilter] = useState(0);

  /** Updates a text/number/select filter field. */
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  /** Validates a numeric field on blur and resets to empty if invalid. */
  const handlePriceBlur = (e) => {
    const { name, value } = e.target;
    if (value === '') return;
    const num = Number(value);
    if (num < VALIDATION.MIN_PRICE) {
      toast.error(`${name === 'minPrice' ? 'Min' : 'Max'} price cannot be negative`);
      setFilters((prev) => ({ ...prev, [name]: '' }));
    } else if (num > VALIDATION.MAX_PRICE) {
      toast.error(`Price cannot exceed $${VALIDATION.MAX_PRICE}`);
      setFilters((prev) => ({ ...prev, [name]: '' }));
    }
  };

  /** Toggles a service-type checkbox. */
  const handleServiceTypeToggle = (value) => {
    setServiceTypeChecks((prev) => ({ ...prev, [value]: !prev[value] }));
  };

  /** Validates filter values before submitting the search. */
  const validateFilters = () => {
    const { latitude, longitude, radiusKm, minPrice, maxPrice, startTime, endTime } = filters;
    if (latitude !== '' && (Number(latitude) < VALIDATION.MIN_LATITUDE || Number(latitude) > VALIDATION.MAX_LATITUDE)) {
      toast.error('Latitude must be between -90 and 90');
      return false;
    }
    if (longitude !== '' && (Number(longitude) < VALIDATION.MIN_LONGITUDE || Number(longitude) > VALIDATION.MAX_LONGITUDE)) {
      toast.error('Longitude must be between -180 and 180');
      return false;
    }
    if (radiusKm !== '' && (Number(radiusKm) < VALIDATION.MIN_RADIUS_KM || Number(radiusKm) > VALIDATION.MAX_RADIUS_KM)) {
      toast.error(`Radius must be between ${VALIDATION.MIN_RADIUS_KM} and ${VALIDATION.MAX_RADIUS_KM} km`);
      return false;
    }
    if (minPrice !== '' && Number(minPrice) < VALIDATION.MIN_PRICE) {
      toast.error('Min price cannot be negative');
      return false;
    }
    if (maxPrice !== '' && Number(maxPrice) < VALIDATION.MIN_PRICE) {
      toast.error('Max price cannot be negative');
      return false;
    }
    if (maxPrice !== '' && Number(maxPrice) > VALIDATION.MAX_PRICE) {
      toast.error(`Max price cannot exceed $${VALIDATION.MAX_PRICE}`);
      return false;
    }
    if (minPrice !== '' && maxPrice !== '' && Number(minPrice) > Number(maxPrice)) {
      toast.error('Min price cannot be greater than max price');
      return false;
    }
    if (startTime && new Date(startTime) < new Date()) {
      toast.error('Start time must be in the future');
      return false;
    }
    if (startTime && endTime && new Date(endTime) <= new Date(startTime)) {
      toast.error('End time must be after start time');
      return false;
    }
    return true;
  };

  /** Submits the search with current filter state. */
  const handleApplyFilters = useCallback(async () => {
    if (!validateFilters()) return;

    setIsLoading(true);
    setHasSearched(true);

    const params = {};
    Object.entries(filters).forEach(([key, val]) => {
      if (val !== '' && val !== null && val !== undefined) params[key] = val;
    });

    // Derive serviceType from checkboxes (last checked wins; if both or none, omit)
    const checkedTypes = SERVICE_TYPE_OPTIONS.filter((o) => serviceTypeChecks[o.value]).map((o) => o.value);
    if (checkedTypes.length === 1) params.serviceType = checkedTypes[0];

    try {
      const { data } = await searchVehicles(params);
      const filtered = ownerRatingFilter > 0
        ? data.filter((v) => Number(v.ownerRating ?? 0) >= ownerRatingFilter)
        : data;
      setResults(filtered);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, [filters, serviceTypeChecks, ownerRatingFilter]);

  const nowLocal = new Date();
  const minDateTime = `${nowLocal.getFullYear()}-${String(nowLocal.getMonth() + 1).padStart(2, '0')}-${String(nowLocal.getDate()).padStart(2, '0')}T${String(nowLocal.getHours()).padStart(2, '0')}:${String(nowLocal.getMinutes()).padStart(2, '0')}`;
  const sortedResults = sortVehicles(results, sortKey);

  return (
    <div className="flex flex-1 min-h-0 bg-bg-light">
      {/* ── Left: Filter Sidebar ─────────────────────────────────── */}
      <aside className="w-64 shrink-0 bg-white border-r border-border overflow-y-auto p-5 hidden md:block">
        <h2 className="text-sm font-bold text-text-dark uppercase tracking-wide mb-4">Filters</h2>

        {/* Price Range */}
        <FilterSection title="Price Range ($/hr)">
          <div className="flex gap-2">
            <input
              type="number"
              name="minPrice"
              value={filters.minPrice}
              onChange={handleFilterChange}
              onBlur={handlePriceBlur}
              min="0"
              step="0.01"
              placeholder="Min"
              className="w-full px-2 py-1.5 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
            />
            <input
              type="number"
              name="maxPrice"
              value={filters.maxPrice}
              onChange={handleFilterChange}
              onBlur={handlePriceBlur}
              min="0"
              step="0.01"
              placeholder="Max"
              className="w-full px-2 py-1.5 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
            />
          </div>
        </FilterSection>

        {/* Shift Duration */}
        <FilterSection title="Shift Duration">
          <div className="space-y-2">
            <div>
              <label className="block text-xs text-text-gray mb-1">Start</label>
              <input
                type="datetime-local"
                name="startTime"
                value={filters.startTime}
                min={minDateTime}
                onChange={handleFilterChange}
                className="w-full px-2 py-1.5 border-2 border-gray-200 rounded-lg text-xs focus:outline-none focus:border-accent-orange"
              />
            </div>
            <div>
              <label className="block text-xs text-text-gray mb-1">End</label>
              <input
                type="datetime-local"
                name="endTime"
                value={filters.endTime}
                min={filters.startTime || minDateTime}
                onChange={handleFilterChange}
                className="w-full px-2 py-1.5 border-2 border-gray-200 rounded-lg text-xs focus:outline-none focus:border-accent-orange"
              />
            </div>
          </div>
        </FilterSection>

        {/* Service Type */}
        <FilterSection title="Service Type">
          {SERVICE_TYPE_OPTIONS.map((opt) => (
            <label key={opt.value} className="flex items-center gap-2 text-sm text-text-dark cursor-pointer mb-1.5">
              <input
                type="checkbox"
                checked={serviceTypeChecks[opt.value]}
                onChange={() => handleServiceTypeToggle(opt.value)}
                className="accent-accent-orange"
              />
              {opt.label}
            </label>
          ))}
        </FilterSection>

        {/* Owner Rating */}
        <FilterSection title="Owner Rating">
          <div className="flex gap-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                onClick={() => setOwnerRatingFilter(ownerRatingFilter === star ? 0 : star)}
                className={`text-lg transition-colors ${
                  star <= ownerRatingFilter ? 'text-turbo-yellow' : 'text-gray-300 hover:text-turbo-yellow'
                }`}
                title={`${star} star${star !== 1 ? 's' : ''} and above`}
              >
                <Star size={20} fill={star <= ownerRatingFilter ? 'currentColor' : 'none'} />
              </button>
            ))}
            {ownerRatingFilter > 0 && (
              <span className="text-xs text-text-gray self-center ml-1">{ownerRatingFilter}+</span>
            )}
          </div>
        </FilterSection>

        {/* Apply Filters Button */}
        <button
          type="button"
          onClick={handleApplyFilters}
          disabled={isLoading}
          className="w-full flex items-center justify-center gap-2 py-2.5 mt-2 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
        >
          {isLoading ? <Loader2 size={15} className="animate-spin" /> : null}
          {isLoading ? 'Searching...' : 'Apply Filters'}
        </button>
      </aside>

      {/* ── Center: Available Cars Listing ───────────────────────── */}
      <section className="flex-1 min-w-0 flex flex-col px-5 py-6 overflow-y-auto">
        {/* Listing header */}
        <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
          <h1 className="text-xl font-bold text-text-dark">Available Cars Listing</h1>

          {/* Sort buttons */}
          <div className="flex gap-2">
            {SORT_OPTIONS.map((opt) => (
              <button
                key={opt.key}
                type="button"
                onClick={() => setSortKey(opt.key)}
                className={`px-3 py-1.5 text-xs font-semibold rounded-full border-2 transition-colors ${
                  sortKey === opt.key
                    ? 'bg-accent-orange text-white border-accent-orange'
                    : 'border-gray-200 text-text-gray hover:border-accent-orange'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Mobile filter note */}
        <div className="md:hidden mb-4">
          <button
            type="button"
            onClick={handleApplyFilters}
            disabled={isLoading}
            className="w-full py-2 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
          >
            {isLoading ? 'Searching...' : 'Search Vehicles'}
          </button>
        </div>

        {/* Results count */}
        {!isLoading && results.length > 0 && (
          <p className="text-sm text-text-gray mb-3">
            {results.length} vehicle{results.length !== 1 ? 's' : ''} found
          </p>
        )}

        {/* Loading state */}
        {isLoading && (
          <div className="flex justify-center py-16">
            <Loader2 size={32} className="animate-spin text-accent-orange" />
          </div>
        )}

        {/* Empty state */}
        {!isLoading && hasSearched && results.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <Car size={48} className="text-gray-300 mb-4" />
            <p className="text-text-gray">{EMPTY_STATE_MESSAGE}</p>
          </div>
        )}

        {/* Initial call-to-action */}
        {!isLoading && !hasSearched && (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <MapPin size={48} className="text-gray-300 mb-4" />
            <p className="text-text-gray font-medium">Set your filters and click Apply Filters to find vehicles near you.</p>
          </div>
        )}

        {/* Vehicle cards grid */}
        {!isLoading && sortedResults.length > 0 && (
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
            {sortedResults.map((vehicle) => (
              <VehicleSearchCard
                key={vehicle.vehicleId}
                vehicle={vehicle}
                onClick={() => setSelectedVehicle(vehicle)}
              />
            ))}
          </div>
        )}
      </section>

      {/* ── Right: Map Placeholder ────────────────────────────────── */}
      <aside className="hidden lg:flex w-72 shrink-0 items-stretch">
        <div className="flex-1 bg-accent-orange/10 border-l border-border flex flex-col items-center justify-center gap-3 p-6">
          <div className="w-full h-full min-h-64 bg-accent-orange rounded-xl flex flex-col items-center justify-center gap-2 shadow-inner">
            <MapPin size={36} className="text-white" />
            <span className="text-white text-2xl font-extrabold tracking-widest">MAP</span>
            <p className="text-white/80 text-xs text-center px-4">
              Google Maps integration coming soon
            </p>
          </div>
        </div>
      </aside>

      {/* Vehicle Detail Modal */}
      {selectedVehicle && (
        <VehicleDetailModal
          vehicleId={selectedVehicle.vehicleId}
          onClose={() => setSelectedVehicle(null)}
        />
      )}
    </div>
  );
}

/**
 * Wrapper for a labeled filter section.
 * @param {Object} props
 * @param {string} props.title
 * @param {React.ReactNode} props.children
 */
function FilterSection({ title, children }) {
  return (
    <div className="mb-5">
      <p className="text-xs font-bold text-text-dark uppercase tracking-wide mb-2">{title}</p>
      {children}
    </div>
  );
}
