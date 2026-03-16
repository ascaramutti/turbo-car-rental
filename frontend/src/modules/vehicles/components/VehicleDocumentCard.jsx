import { useState } from 'react';
import { Upload, CheckCircle, XCircle, Clock, RefreshCw, Loader2, Eye, Download } from 'lucide-react';
import toast from 'react-hot-toast';
import {
  uploadVehicleDocument,
  reuploadVehicleDocument,
  getVehicleDocumentViewUrl,
  getVehicleDocumentDownloadUrl,
} from '../api/vehicleApi';
import FilePreviewModal from '../../../shared/components/common/FilePreviewModal';
import { downloadFile } from '../../../shared/utils/downloadUtils';
import { validateDocumentFile } from '../../documents/utils/fileValidation';
import { extractErrorMessage } from '../../auth/utils/validation';
import {
  DOCUMENT_STATUS,
  DOCUMENT_STATUS_CONFIG,
  VEHICLE_DOCUMENT_LABELS,
} from '../constants/vehicleConstants';
import { FILE_CONSTRAINTS } from '../../documents/constants/documentConstants';

/** Maps document statuses to their corresponding Lucide icons. */
const STATUS_ICONS = {
  [DOCUMENT_STATUS.PENDING]: Clock,
  [DOCUMENT_STATUS.APPROVED]: CheckCircle,
  [DOCUMENT_STATUS.REJECTED]: XCircle,
};

/**
 * Displays an upload card for a single vehicle document type, or shows its review status if uploaded.
 * Reuses FilePreviewModal for viewing and downloadFile for downloading.
 * @param {Object} props
 * @param {number} props.vehicleId - ID of the vehicle this document belongs to
 * @param {string} props.documentType - INSURANCE, VEHICLE_REGISTRATION, or INSPECTION_REPORT
 * @param {Object|null} props.document - Uploaded document object or null
 * @param {Function} props.onUpdate - Callback triggered after successful upload/reupload
 */
export default function VehicleDocumentCard({ vehicleId, documentType, document, onUpdate }) {
  const [isUploading, setIsUploading] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);

  const status = document?.status;
  const config = status ? DOCUMENT_STATUS_CONFIG[status] : null;
  const StatusIcon = status ? STATUS_ICONS[status] : null;
  const label = VEHICLE_DOCUMENT_LABELS[documentType];

  /** Validates and uploads or re-uploads a file. */
  const handleFileSelected = async (e, isReupload) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const error = validateDocumentFile(file);
    if (error) {
      toast.error(error);
      e.target.value = '';
      return;
    }

    setIsUploading(true);
    try {
      if (isReupload) {
        await reuploadVehicleDocument(vehicleId, document.documentId, file);
      } else {
        await uploadVehicleDocument(vehicleId, file, documentType);
      }
      toast.success(`${label} ${isReupload ? 're-uploaded' : 'uploaded'} successfully`);
      onUpdate();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsUploading(false);
      e.target.value = '';
    }
  };

  /** Downloads the file via authenticated API call. */
  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      await downloadFile(
        getVehicleDocumentDownloadUrl(vehicleId, document.documentId),
        document.fileName
      );
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsDownloading(false);
    }
  };

  // ── Not uploaded yet ──────────────────────────────────────────
  if (!document) {
    return (
      <div className="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center hover:border-accent-orange transition-colors">
        <Upload className="mx-auto mb-3 text-gray-400" size={32} />
        <p className="text-sm font-bold text-text-dark mb-1">{label}</p>
        <p className="text-xs text-text-gray mb-4">
          PDF, JPG, or PNG (max {FILE_CONSTRAINTS.MAX_SIZE_MB}MB)
        </p>
        <label className="cursor-pointer inline-flex items-center gap-2 px-4 py-2 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors">
          {isUploading && <Loader2 size={16} className="animate-spin" />}
          {isUploading ? 'Uploading...' : 'Select File'}
          <input
            type="file"
            accept={FILE_CONSTRAINTS.ACCEPTED_FORMATS}
            onChange={(e) => handleFileSelected(e, false)}
            className="hidden"
            disabled={isUploading}
          />
        </label>
      </div>
    );
  }

  // ── Uploaded — show status ────────────────────────────────────
  const canReplace = status === DOCUMENT_STATUS.PENDING || status === DOCUMENT_STATUS.REJECTED;

  return (
    <div className={`border-2 rounded-xl p-6 ${config.border} ${config.bg}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-bold text-text-dark">{label}</p>
          <p className="text-xs text-text-gray mt-1">{document.fileName}</p>
        </div>
        <div className="flex items-center gap-1.5">
          <StatusIcon size={18} className={config.color} />
          <span className={`text-sm font-semibold ${config.color}`}>{config.label}</span>
        </div>
      </div>

      {status === DOCUMENT_STATUS.REJECTED && document.rejectionReason && (
        <p className="mt-3 text-xs text-red-600 bg-red-100 rounded-lg p-2">
          Reason: {document.rejectionReason}
        </p>
      )}

      {/* Action buttons */}
      <div className="mt-4 flex items-center gap-3">
        <button
          type="button"
          onClick={() => setShowPreview(true)}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 border-2 border-driver-blue text-driver-blue text-xs font-semibold rounded-full hover:bg-driver-blue/5 transition-colors"
        >
          <Eye size={14} />
          View Document
        </button>

        <button
          type="button"
          onClick={handleDownload}
          disabled={isDownloading}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 border-2 border-gray-300 text-text-dark text-xs font-semibold rounded-full hover:border-accent-orange hover:text-accent-orange transition-colors disabled:opacity-50"
        >
          {isDownloading ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
          {isDownloading ? 'Downloading...' : 'Download'}
        </button>

        {canReplace && (
          <label className="cursor-pointer inline-flex items-center gap-2 px-3 py-1.5 border-2 border-gray-300 text-text-dark text-xs font-semibold rounded-full hover:border-accent-orange hover:text-accent-orange transition-colors">
            {isUploading ? <Loader2 size={14} className="animate-spin" /> : <RefreshCw size={14} />}
            {isUploading ? 'Uploading...' : 'Replace File'}
            <input
              type="file"
              accept={FILE_CONSTRAINTS.ACCEPTED_FORMATS}
              onChange={(e) => handleFileSelected(e, true)}
              className="hidden"
              disabled={isUploading}
            />
          </label>
        )}
      </div>

      {showPreview && (
        <FilePreviewModal
          fileUrl={getVehicleDocumentViewUrl(vehicleId, document.documentId)}
          fileName={document.fileName}
          onClose={() => setShowPreview(false)}
        />
      )}
    </div>
  );
}
