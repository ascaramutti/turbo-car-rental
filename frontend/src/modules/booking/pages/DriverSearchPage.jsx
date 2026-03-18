import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Loader2, Car, SlidersHorizontal, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { searchVehicles } from '../api/bookingApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import VehicleSearchCard from '../components/VehicleSearchCard';

/** Vehicle category options for the filter dropdown. */
const CATEGORY_OPTIONS = ['', 'SEDAN', 'SUV', 'VAN', 'TRUCK', 'COMPACT'];

/** Service type options for the filter dropdown. */
const SERVICE_TYPE_OPTIONS = ['', 'TAXI_AND_DELIVERY', 'DELIVERY_ONLY'];

/** Fuel type options for the filter dropdown. */
const FUEL_TYPE_OPTIONS = ['', 'GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID'];

/** Empty-state message displayed when the search yields no results. */
const EMPTY_STATE_MESSAGE = 'No vehicles found. Try adjusting your filters or search radius.';

/**
 * Page where drivers search for available vehicles.
 * Supports location-based search, time filters, and category/fuel/price filters.
 */
export default function DriverSearchPage() {
  const navigate = useNavigate();

  const [results, setResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [showFilters, setShowFilters] = useState(false);

  const [filters, setFilters] = useState({
    latitude: '',
    longitude: '',
    radiusKm: '10',
    startTime: '',
    endTime: '',
    category: '',
    serviceType: '',
    fuelType: '',
    minPrice: '',
    maxPrice: '',
  });

  /** Updates a filter field value. */
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  /** Clears all filter values. */
  const handleClearFilters = () => {
    setFilters({
      latitude: '',
      longitude: '',
      radiusKm: '10',
      startTime: '',
      endTime: '',
      category: '',
      serviceType: '',
      fuelType: '',
      minPrice: '',
      maxPrice: '',
    });
  };

  /** Submits the search by calling the API with the current filter values. */
  const handleSearch = useCallback(async (e) => {
    if (e) e.preventDefault();
    setIsLoading(true);
    setHasSearched(true);

    // Build params, omitting empty strings
    const params = {};
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '' && value != null) params[key] = value;
    });

    try {
      const { data } = await searchVehicles(params);
      setResults(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  const minDateTime = new Date().toISOString().slice(0, 16);

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-text-dark">Find a Vehicle</h1>
          <p className="text-text-gray mt-1">Search available vehicles near you</p>
        </div>

        {/* Search form */}
        <form onSubmit={handleSearch} className="bg-white border-2 border-gray-200 rounded-xl p-5 mb-6">
          {/* Location + time row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-3">
            <div>
              <label className="block text-xs font-semibold text-text-dark mb-1">Start Time</label>
              <input
                type="datetime-local"
                name="startTime"
                value={filters.startTime}
                min={minDateTime}
                onChange={handleFilterChange}
                className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-dark mb-1">End Time</label>
              <input
                type="datetime-local"
                name="endTime"
                value={filters.endTime}
                min={filters.startTime || minDateTime}
                onChange={handleFilterChange}
                className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
              />
            </div>
          </div>

          {/* Location row */}
          <div className="grid grid-cols-3 gap-3 mb-3">
            <div>
              <label className="block text-xs font-semibold text-text-dark mb-1">Latitude</label>
              <input
                type="number"
                name="latitude"
                value={filters.latitude}
                onChange={handleFilterChange}
                step="any"
                placeholder="e.g. 49.28"
                className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-dark mb-1">Longitude</label>
              <input
                type="number"
                name="longitude"
                value={filters.longitude}
                onChange={handleFilterChange}
                step="any"
                placeholder="e.g. -123.12"
                className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-dark mb-1">Radius (km)</label>
              <input
                type="number"
                name="radiusKm"
                value={filters.radiusKm}
                onChange={handleFilterChange}
                min="1"
                max="100"
                step="1"
                className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
              />
            </div>
          </div>

          {/* Toggle advanced filters */}
          <button
            type="button"
            onClick={() => setShowFilters((v) => !v)}
            className="inline-flex items-center gap-1.5 text-sm text-text-gray hover:text-accent-orange transition-colors mb-3"
          >
            <SlidersHorizontal size={15} />
            {showFilters ? 'Hide' : 'Show'} advanced filters
          </button>

          {showFilters && (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mb-3">
              <div>
                <label className="block text-xs font-semibold text-text-dark mb-1">Category</label>
                <select
                  name="category"
                  value={filters.category}
                  onChange={handleFilterChange}
                  className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
                >
                  {CATEGORY_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt || 'Any category'}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-text-dark mb-1">Service Type</label>
                <select
                  name="serviceType"
                  value={filters.serviceType}
                  onChange={handleFilterChange}
                  className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
                >
                  {SERVICE_TYPE_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt || 'Any type'}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-text-dark mb-1">Fuel Type</label>
                <select
                  name="fuelType"
                  value={filters.fuelType}
                  onChange={handleFilterChange}
                  className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
                >
                  {FUEL_TYPE_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt || 'Any fuel'}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-text-dark mb-1">Min Price ($/hr)</label>
                <input
                  type="number"
                  name="minPrice"
                  value={filters.minPrice}
                  onChange={handleFilterChange}
                  min="0"
                  step="0.01"
                  placeholder="No min"
                  className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-text-dark mb-1">Max Price ($/hr)</label>
                <input
                  type="number"
                  name="maxPrice"
                  value={filters.maxPrice}
                  onChange={handleFilterChange}
                  min="0"
                  step="0.01"
                  placeholder="No max"
                  className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:border-accent-orange"
                />
              </div>
              <div className="flex items-end">
                <button
                  type="button"
                  onClick={handleClearFilters}
                  className="inline-flex items-center gap-1.5 px-3 py-2 border-2 border-gray-200 text-text-gray text-xs font-semibold rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <X size={13} />
                  Clear filters
                </button>
              </div>
            </div>
          )}

          {/* Search button */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full flex items-center justify-center gap-2 py-2.5 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
          >
            {isLoading ? <Loader2 size={16} className="animate-spin" /> : <Search size={16} />}
            {isLoading ? 'Searching...' : 'Search Vehicles'}
          </button>
        </form>

        {/* Results */}
        {isLoading && (
          <div className="flex justify-center py-12">
            <Loader2 size={32} className="animate-spin text-accent-orange" />
          </div>
        )}

        {!isLoading && hasSearched && results.length === 0 && (
          <div className="text-center py-16">
            <Car size={48} className="mx-auto text-gray-300 mb-4" />
            <p className="text-text-gray">{EMPTY_STATE_MESSAGE}</p>
          </div>
        )}

        {!isLoading && results.length > 0 && (
          <>
            <p className="text-sm text-text-gray mb-3">{results.length} vehicle{results.length !== 1 ? 's' : ''} found</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {results.map((vehicle) => (
                <VehicleSearchCard
                  key={vehicle.vehicleId}
                  vehicle={vehicle}
                  onClick={() => navigate(`/driver/search/${vehicle.vehicleId}`)}
                />
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
