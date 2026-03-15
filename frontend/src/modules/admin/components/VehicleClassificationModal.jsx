import { useState } from 'react';
import { Car, CheckCircle, Loader2, AlertTriangle, Eye, FileCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import { approveVehicle } from '../api/adminVehicleApi';
import { getAdminDocumentViewUrl } from '../api/adminDocumentApi';
import FilePreviewModal from '../../../shared/components/common/FilePreviewModal';
import { extractErrorMessage } from '../../auth/utils/validation';
import { SERVICE_TYPE_LABELS, SERVICE_TYPES, VEHICLE_DOCUMENT_LABELS } from '../../vehicles/constants/vehicleConstants';

/**
 * Modal for admin to classify a vehicle after all documents are approved.
 * @param {Object} classificationData - From classification-check API
 * @param {Function} onClassified - Callback after successful classification
 * @param {Function} onClose - Callback to close modal
 */
export default function VehicleClassificationModal({ classificationData, onClassified, onClose }) {
  const [selectedType, setSelectedType] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [previewDoc, setPreviewDoc] = useState(null);

  const { vehicleId, vehicleYear, vehicleMake, vehicleModel, availableServiceTypes, documents } = classificationData;
  const hasOnlyOneOption = availableServiceTypes.length === 1;

  /** Submits the classification to the backend. */
  const handleClassify = async () => {
    const serviceType = hasOnlyOneOption ? availableServiceTypes[0] : selectedType;
    if (!serviceType) {
      toast.error('Please select a service type');
      return;
    }

    setIsSubmitting(true);
    try {
      await approveVehicle(vehicleId, serviceType);
      toast.success(`Vehicle classified as ${SERVICE_TYPE_LABELS[serviceType]}`);
      onClassified();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md shadow-xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
              <Car size={20} className="text-green-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-text-dark">Classify Vehicle</h2>
              <p className="text-xs text-text-gray">All documents approved</p>
            </div>
          </div>
        </div>

        <div className="p-6">
          {/* Vehicle Info */}
          <div className="bg-gray-50 rounded-lg p-4 mb-5">
            <p className="text-sm font-bold text-text-dark">{vehicleYear} {vehicleMake} {vehicleModel}</p>
            <p className="text-xs text-text-gray mt-1">Vehicle ID: {vehicleId}</p>
          </div>

          {/* Approved Documents */}
          {documents && documents.length > 0 && (
            <div className="mb-5">
              <p className="text-xs font-bold text-text-dark mb-2">Approved Documents</p>
              <div className="space-y-1.5">
                {documents.map((doc) => (
                  <div key={doc.documentId} className="flex items-center justify-between bg-green-50 border border-green-200 rounded-lg px-3 py-2">
                    <div className="flex items-center gap-2">
                      <FileCheck size={14} className="text-green-600" />
                      <span className="text-xs font-medium text-text-dark">
                        {VEHICLE_DOCUMENT_LABELS[doc.documentType] || doc.documentType}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setPreviewDoc(doc)}
                      className="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold text-driver-blue hover:bg-blue-50 rounded transition-colors"
                    >
                      <Eye size={12} />
                      View
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Service Type Selection */}
          <p className="text-sm font-bold text-text-dark mb-3">Service Type</p>

          {hasOnlyOneOption ? (
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-amber-600 bg-amber-50 rounded-lg p-3">
                <AlertTriangle size={16} />
                <p className="text-xs font-medium">
                  {!classificationData.hasApprovedInspection
                    ? 'No inspection report — only Delivery service available'
                    : 'Vehicle age exceeds taxi eligibility — only Delivery service available'}
                </p>
              </div>
              <div className="border-2 border-blue-500 bg-blue-50 rounded-lg p-3">
                <p className="text-sm font-semibold text-blue-700">
                  {SERVICE_TYPE_LABELS[availableServiceTypes[0]]}
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-2">
              {availableServiceTypes.map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setSelectedType(type)}
                  className={`w-full text-left p-3 rounded-lg border-2 transition-all ${
                    selectedType === type
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <p className={`text-sm font-semibold ${selectedType === type ? 'text-blue-700' : 'text-text-dark'}`}>
                    {SERVICE_TYPE_LABELS[type]}
                  </p>
                  <p className="text-xs text-text-gray mt-0.5">
                    {type === SERVICE_TYPES.TAXI_AND_DELIVERY
                      ? 'Vehicle can be used for taxi and delivery services'
                      : 'Vehicle can only be used for delivery services'}
                  </p>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Action */}
        <div className="p-6 border-t border-gray-100">
          <button
            onClick={handleClassify}
            disabled={isSubmitting || (!hasOnlyOneOption && !selectedType)}
            className="w-full flex items-center justify-center gap-2 py-2.5 bg-green-600 text-white text-sm font-bold rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
          >
            {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle size={16} />}
            Classify Vehicle
          </button>
        </div>
      </div>

      {previewDoc && (
        <FilePreviewModal
          fileUrl={getAdminDocumentViewUrl(previewDoc.documentId)}
          fileName={previewDoc.fileName}
          onClose={() => setPreviewDoc(null)}
        />
      )}
    </div>
  );
}
