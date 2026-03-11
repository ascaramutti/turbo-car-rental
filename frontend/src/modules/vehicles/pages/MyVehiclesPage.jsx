import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Loader2, Car, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { getMyVehicles, registerVehicle } from '../api/vehicleApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import VehicleCard from '../components/VehicleCard';
import VehicleForm from '../components/VehicleForm';

/** Page title displayed in the header. */
const PAGE_TITLE = 'My Vehicles';

/** Subtitle displayed below the page title. */
const PAGE_SUBTITLE = 'Manage your vehicles, documents, and availability';

/** Empty-state message shown when the owner has no vehicles. */
const EMPTY_STATE_MESSAGE = 'You have not registered any vehicles yet. Click the button above to get started.';

/** Success toast message after registering a vehicle. */
const REGISTER_SUCCESS_MESSAGE = 'Vehicle registered successfully';

/**
 * Page listing all vehicles belonging to the authenticated car owner.
 * Provides a modal to register new vehicles and links to individual vehicle detail pages.
 */
export default function MyVehiclesPage() {
  const [vehicles, setVehicles] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  /** Fetches the owner's vehicles from the API. */
  const fetchVehicles = useCallback(async () => {
    try {
      const { data } = await getMyVehicles();
      setVehicles(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchVehicles();
  }, [fetchVehicles]);

  /** Handles new vehicle registration. */
  const handleRegister = async (data) => {
    setIsSubmitting(true);
    try {
      const { data: newVehicle } = await registerVehicle(data);
      toast.success(REGISTER_SUCCESS_MESSAGE);
      setShowForm(false);
      fetchVehicles();
      navigate(`/owner/vehicles/${newVehicle.vehicleId}`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-text-dark">{PAGE_TITLE}</h1>
            <p className="text-text-gray mt-1">{PAGE_SUBTITLE}</p>
          </div>
          <button
            type="button"
            onClick={() => setShowForm(true)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors"
          >
            <Plus size={18} />
            Register New Vehicle
          </button>
        </div>

        {/* Vehicle Grid */}
        {vehicles.length === 0 ? (
          <div className="text-center py-16">
            <Car size={48} className="mx-auto text-gray-300 mb-4" />
            <p className="text-text-gray">{EMPTY_STATE_MESSAGE}</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {vehicles.map((vehicle) => (
              <VehicleCard
                key={vehicle.vehicleId}
                vehicle={vehicle}
                onClick={() => navigate(`/owner/vehicles/${vehicle.vehicleId}`)}
              />
            ))}
          </div>
        )}

        {/* Register Vehicle Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-auto">
              <div className="flex items-center justify-between p-6 border-b">
                <h2 className="text-lg font-bold text-text-dark">Register New Vehicle</h2>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <X size={20} className="text-text-gray" />
                </button>
              </div>
              <div className="p-6">
                <VehicleForm onSubmit={handleRegister} isLoading={isSubmitting} />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
