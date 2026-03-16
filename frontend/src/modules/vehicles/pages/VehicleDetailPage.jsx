import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Loader2, ArrowLeft, X, Edit3, FileText, Power, PowerOff } from 'lucide-react';
import toast from 'react-hot-toast';
import {
  getVehicleById,
  updateVehicle,
  activateVehicle,
  deactivateVehicle,
  getVehicleDocuments,
} from '../api/vehicleApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { validateActivateForm } from '../utils/vehicleValidation';
import VehicleForm from '../components/VehicleForm';
import VehicleDocumentCard from '../components/VehicleDocumentCard';
import {
  STATUS_CONFIG,
  VEHICLE_STATUS,
  CATEGORY_LABELS,
  FUEL_TYPE_LABELS,
  SERVICE_TYPE_LABELS,
  REQUIRED_VEHICLE_DOCUMENT_TYPES,
  OPTIONAL_VEHICLE_DOCUMENT_TYPES,
  ALL_VEHICLE_DOCUMENT_TYPES,
  VEHICLE_DOCUMENT_LABELS,
  MIN_HOURLY_RATE,
} from '../constants/vehicleConstants';
import { FILE_CONSTRAINTS } from '../../documents/constants/documentConstants';

/** Success message after vehicle update. */
const UPDATE_SUCCESS_MESSAGE = 'Vehicle updated successfully';

/** Success message after listing vehicle for rent. */
const ACTIVATE_SUCCESS_MESSAGE = 'Vehicle is now listed for rent';

/** Success message after removing vehicle from rent. */
const DEACTIVATE_SUCCESS_MESSAGE = 'Vehicle removed from rent listings';

/** Confirmation prompt for deactivation. */
const DEACTIVATE_CONFIRM_MESSAGE = 'Are you sure you want to deactivate this vehicle? This action cannot be undone.';

/**
 * Page showing the full details of a single vehicle.
 * Includes vehicle info (editable), vehicle documents, and activate/deactivate with availableUntil.
 */
export default function VehicleDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [vehicle, setVehicle] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showEditForm, setShowEditForm] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isActivating, setIsActivating] = useState(false);
  const [isDeactivating, setIsDeactivating] = useState(false);
  const [availableUntil, setAvailableUntil] = useState('');
  const [activateLocation, setActivateLocation] = useState('');
  const [activateLatitude, setActivateLatitude] = useState('');
  const [activateLongitude, setActivateLongitude] = useState('');
  const [activateHourlyRate, setActivateHourlyRate] = useState('');

  /** Fetches vehicle details from the API. */
  const fetchVehicle = useCallback(async () => {
    try {
      const { data } = await getVehicleById(id);
      setVehicle(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
      navigate('/owner/vehicles');
    }
  }, [id, navigate]);

  /** Fetches vehicle documents from the API. */
  const fetchDocuments = useCallback(async () => {
    try {
      const { data } = await getVehicleDocuments(id);
      setDocuments(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }, [id]);

  /** Loads all data on mount. */
  useEffect(() => {
    const loadAll = async () => {
      await Promise.all([fetchVehicle(), fetchDocuments()]);
      setIsLoading(false);
    };
    loadAll();
  }, [fetchVehicle, fetchDocuments]);

  /** Handles vehicle update submission. */
  const handleUpdate = async (data) => {
    setIsUpdating(true);
    try {
      await updateVehicle(id, data);
      toast.success(UPDATE_SUCCESS_MESSAGE);
      setShowEditForm(false);
      fetchVehicle();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsUpdating(false);
    }
  };

  /** Lists the vehicle for rent with availableUntil and location. */
  const handleActivate = async () => {
    const activateData = {
      availableUntil,
      generalLocation: activateLocation,
      latitude: activateLatitude !== '' ? Number(activateLatitude) : null,
      longitude: activateLongitude !== '' ? Number(activateLongitude) : null,
      hourlyRate: activateHourlyRate !== '' ? Number(activateHourlyRate) : null,
    };
    const errors = validateActivateForm(activateData);
    if (Object.keys(errors).length > 0) {
      toast.error(Object.values(errors)[0]);
      return;
    }
    setIsActivating(true);
    try {
      await activateVehicle(id, activateData);
      toast.success(ACTIVATE_SUCCESS_MESSAGE);
      fetchVehicle();
      setAvailableUntil('');
      setActivateLocation('');
      setActivateLatitude('');
      setActivateLongitude('');
      setActivateHourlyRate('');
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsActivating(false);
    }
  };

  /** Removes the vehicle from rent listings. */
  const handleDeactivate = async () => {
    setIsDeactivating(true);
    try {
      await deactivateVehicle(id);
      toast.success(DEACTIVATE_SUCCESS_MESSAGE);
      fetchVehicle();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsDeactivating(false);
    }
  };

  /** Finds the document object for a given type, or null if not uploaded. */
  const getDocumentByType = (type) =>
    documents.find((d) => d.documentType === type) || null;

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  if (!vehicle) return null;

  const statusConfig = STATUS_CONFIG[vehicle.status];

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Back Button */}
        <button
          type="button"
          onClick={() => navigate('/owner/vehicles')}
          className="inline-flex items-center gap-2 text-sm text-text-gray hover:text-text-dark transition-colors mb-6"
        >
          <ArrowLeft size={16} />
          Back to My Vehicles
        </button>

        {/* ── Vehicle Info Section ──────────────────────────────────── */}
        <section className="bg-white border-2 border-gray-200 rounded-xl p-6 mb-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-2xl font-bold text-text-dark">
                {vehicle.year} {vehicle.make} {vehicle.model}
              </h1>
              <p className="text-text-gray mt-1">{vehicle.licensePlate}</p>
            </div>
            <div className="flex items-center gap-3">
              {statusConfig && (
                <span
                  className={`inline-flex items-center px-3 py-1 text-sm font-semibold rounded-full ${statusConfig.bg} ${statusConfig.color} ${statusConfig.border} border`}
                >
                  {statusConfig.label}
                </span>
              )}
              <button
                type="button"
                onClick={() => setShowEditForm(true)}
                className="p-2 text-text-gray hover:text-accent-orange hover:bg-accent-orange/5 rounded-lg transition-colors"
                title="Edit vehicle"
              >
                <Edit3 size={18} />
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
            <InfoItem label="VIN" value={vehicle.vin} />
            <InfoItem label="Category" value={CATEGORY_LABELS[vehicle.category] || vehicle.category} />
            <InfoItem label="Fuel Type" value={FUEL_TYPE_LABELS[vehicle.fuelType] || vehicle.fuelType} />
            {vehicle.isActive && (
              <>
                <InfoItem label="Hourly Rate" value={vehicle.hourlyRate != null ? `$${Number(vehicle.hourlyRate).toFixed(2)}/hr` : 'N/A'} />
                {vehicle.generalLocation && <InfoItem label="Location" value={vehicle.generalLocation} />}
                {vehicle.latitude != null && vehicle.longitude != null && (
                  <InfoItem label="Coordinates" value={`${vehicle.latitude}, ${vehicle.longitude}`} />
                )}
              </>
            )}
          </div>

          {vehicle.description && (
            <div className="mt-4 pt-4 border-t border-gray-100">
              <p className="text-sm text-text-gray">{vehicle.description}</p>
            </div>
          )}

          {/* Rental Status & Actions */}
          <div className="mt-6 pt-4 border-t border-gray-100">
            {vehicle.status === VEHICLE_STATUS.APPROVED && (
              <div className="flex items-center gap-3">
                {vehicle.isActive ? (
                  <>
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 text-sm font-semibold text-green-600 bg-green-50 border border-green-200 rounded-full">
                      <Power size={14} />
                      Listed for Rent
                    </span>
                    {vehicle.serviceType && (
                      <span className="text-xs text-text-gray">
                        Service: {SERVICE_TYPE_LABELS[vehicle.serviceType]}
                      </span>
                    )}
                    {vehicle.availableUntil && (
                      <span className="text-xs text-text-gray">
                        Available until: {vehicle.availableUntil}
                      </span>
                    )}
                    <button
                      type="button"
                      onClick={handleDeactivate}
                      disabled={isDeactivating}
                      className="inline-flex items-center gap-2 px-4 py-2 border-2 border-red-300 text-red-500 text-sm font-semibold rounded-full hover:bg-red-50 transition-colors disabled:opacity-50"
                    >
                      {isDeactivating ? <Loader2 size={16} className="animate-spin" /> : <PowerOff size={16} />}
                      {isDeactivating ? 'Removing...' : 'Remove from Rent'}
                    </button>
                  </>
                ) : (
                  <div className="w-full space-y-3">
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 text-sm font-semibold text-gray-500 bg-gray-50 border border-gray-200 rounded-full">
                      <PowerOff size={14} />
                      Not Listed
                    </span>
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                      <div>
                        <label className="block text-xs font-semibold text-text-dark mb-1">Available Until</label>
                        <input
                          type="datetime-local"
                          value={availableUntil}
                          onChange={(e) => setAvailableUntil(e.target.value)}
                          min={new Date().toISOString().slice(0, 16)}
                          className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-text-dark mb-1">General Location</label>
                        <input
                          type="text"
                          value={activateLocation}
                          onChange={(e) => setActivateLocation(e.target.value)}
                          placeholder="e.g. Downtown Edmonton"
                          className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-text-dark mb-1">Latitude</label>
                        <input
                          type="number"
                          value={activateLatitude}
                          onChange={(e) => setActivateLatitude(e.target.value)}
                          step="any"
                          placeholder="e.g. 49.2827"
                          className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-text-dark mb-1">Longitude</label>
                        <input
                          type="number"
                          value={activateLongitude}
                          onChange={(e) => setActivateLongitude(e.target.value)}
                          step="any"
                          placeholder="e.g. -123.1207"
                          className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-text-dark mb-1">Hourly Rate ($)</label>
                        <input
                          type="number"
                          name="hourlyRate"
                          value={activateHourlyRate}
                          onChange={(e) => setActivateHourlyRate(e.target.value)}
                          min={MIN_HOURLY_RATE}
                          step="0.01"
                          placeholder={`Min $${MIN_HOURLY_RATE.toFixed(2)}`}
                          className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm"
                        />
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={handleActivate}
                      disabled={isActivating || !availableUntil}
                      className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white text-sm font-bold rounded-full hover:bg-green-700 transition-colors disabled:opacity-50"
                    >
                      {isActivating ? <Loader2 size={16} className="animate-spin" /> : <Power size={16} />}
                      {isActivating ? 'Listing...' : 'List for Rent'}
                    </button>
                  </div>
                )}
              </div>
            )}
            {vehicle.status === VEHICLE_STATUS.PENDING && (
              <p className="text-sm text-amber-600">
                Vehicle documents are pending admin approval. You can list this vehicle for rent once all documents are approved.
              </p>
            )}
          </div>
        </section>

        {/* ── Vehicle Documents Section ────────────────────────────── */}
        <section className="bg-white border-2 border-gray-200 rounded-xl p-6 mb-6">
          <div className="flex items-center gap-2 mb-1">
            <FileText size={20} className="text-text-dark" />
            <h2 className="text-lg font-bold text-text-dark">Vehicle Documents</h2>
          </div>
          <p className="text-sm text-text-gray mb-6">
            Upload required documents. Inspection Report is optional but required for Taxi service.
            Accepted formats: PDF, JPG, PNG (max {FILE_CONSTRAINTS.MAX_SIZE_MB}MB).
          </p>

          <div className="space-y-4">
            {ALL_VEHICLE_DOCUMENT_TYPES.map((type) => (
              <div key={type}>
                {REQUIRED_VEHICLE_DOCUMENT_TYPES.includes(type) && (
                  <p className="text-xs text-green-600 font-semibold mb-1">Required</p>
                )}
                {OPTIONAL_VEHICLE_DOCUMENT_TYPES.includes(type) && (
                  <p className="text-xs text-amber-500 font-semibold mb-1">Optional — Required for Taxi + Delivery only</p>
                )}
                <VehicleDocumentCard
                  vehicleId={vehicle.vehicleId}
                  documentType={type}
                  document={getDocumentByType(type)}
                  onUpdate={fetchDocuments}
                />
              </div>
            ))}
          </div>
        </section>

        {/* ── Edit Vehicle Modal ───────────────────────────────────── */}
        {showEditForm && (
          <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-auto">
              <div className="flex items-center justify-between p-6 border-b">
                <h2 className="text-lg font-bold text-text-dark">Edit Vehicle</h2>
                <button
                  type="button"
                  onClick={() => setShowEditForm(false)}
                  className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <X size={20} className="text-text-gray" />
                </button>
              </div>
              <div className="p-6">
                <VehicleForm
                  onSubmit={handleUpdate}
                  initialData={vehicle}
                  isLoading={isUpdating}
                />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Displays a label-value pair in the vehicle info grid.
 * @param {Object} props
 * @param {string} props.label - Field label
 * @param {string} props.value - Field value
 */
function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs text-text-gray font-semibold uppercase tracking-wide">{label}</p>
      <p className="text-text-dark font-medium mt-0.5">{value}</p>
    </div>
  );
}
