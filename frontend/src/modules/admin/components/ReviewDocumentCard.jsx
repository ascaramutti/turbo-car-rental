import { useState } from 'react';
import { CheckCircle, XCircle, Loader2, FileText, User, Eye, Download } from 'lucide-react';
import toast from 'react-hot-toast';
import { reviewDocument, getAdminDocumentViewUrl, getAdminDocumentDownloadUrl } from '../api/adminDocumentApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';
import FilePreviewModal from '../../../shared/components/common/FilePreviewModal';
import { extractErrorMessage } from '../../auth/utils/validation';
import { REVIEW_ACTIONS, LICENSE_CLASSES, LICENSE_CLASS_LABELS, REJECTION_REASON } from '../constants/adminConstants';
import { DOCUMENT_LABELS, DOCUMENT_TYPES } from '../../documents/constants/documentConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';


/**
 * Card for admin to review a single pending document.
 * @param {Object} document - AdminDocumentResponse from API
 * @param {Function} onReviewed - Callback after successful review
 */
export default function ReviewDocumentCard({ document, onReviewed }) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [selectedLicenseClass, setSelectedLicenseClass] = useState(LICENSE_CLASSES.CLASS_4);
  const [isDownloading, setIsDownloading] = useState(false);

  const isLicense = document.documentType === DOCUMENT_TYPES.DRIVERS_LICENSE;

  /** Submits an approval review to the API. */
  const handleApprove = async () => {
    const reviewData = { action: REVIEW_ACTIONS.APPROVE };
    if (isLicense) {
      reviewData.licenseClass = selectedLicenseClass;
    }

    setIsSubmitting(true);
    try {
      await reviewDocument(document.documentId, reviewData);
      toast.success(`${DOCUMENT_LABELS[document.documentType]} approved`);
      onReviewed();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  /** Downloads the document file via authenticated API call. */
  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      await downloadFile(getAdminDocumentDownloadUrl(document.documentId), document.fileName);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsDownloading(false);
    }
  };

  /** Filters input to only allow characters matching the backend pattern. */
  const handleReasonChange = (e) => {
    const value = e.target.value;
    if (REJECTION_REASON.PATTERN.test(value)) {
      setRejectionReason(value);
    }
  };

  /** Submits a rejection review to the API. */
  const handleReject = async () => {
    if (!rejectionReason.trim()) {
      toast.error(REJECTION_REASON.REQUIRED_MESSAGE);
      return;
    }

    setIsSubmitting(true);
    try {
      await reviewDocument(document.documentId, {
        action: REVIEW_ACTIONS.REJECT,
        rejectionReason: rejectionReason.trim(),
      });
      toast.success(`${DOCUMENT_LABELS[document.documentType]} rejected`);
      onReviewed();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white border-2 border-gray-200 rounded-xl p-6 shadow-sm">
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-driver-blue/10 rounded-full flex items-center justify-center">
            <User size={20} className="text-driver-blue" />
          </div>
          <div>
            <p className="font-bold text-text-dark">{document.userFullName}</p>
            <p className="text-xs text-text-gray">User ID: {document.userId}</p>
          </div>
        </div>
        <div className="flex items-center gap-2 bg-gray-100 rounded-lg px-3 py-1.5">
          <FileText size={14} className="text-text-gray" />
          <span className="text-sm font-semibold text-text-dark">{DOCUMENT_LABELS[document.documentType]}</span>
        </div>
      </div>

      {/* File info + view button */}
      <div className="bg-gray-50 rounded-lg p-3 mb-4 flex items-center justify-between">
        <div className="text-xs text-text-gray">
          <p>File: <span className="text-text-dark font-medium">{document.fileName}</span></p>
          <p>Uploaded: <span className="text-text-dark font-medium">{formatDateTime(document.uploadedAt)}</span></p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowPreview(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-driver-blue text-white text-xs font-bold rounded-lg hover:bg-driver-blue/90 transition-colors"
          >
            <Eye size={14} />
            View
          </button>
          <button
            type="button"
            onClick={handleDownload}
            disabled={isDownloading}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 border-2 border-gray-300 text-text-gray text-xs font-bold rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            {isDownloading ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
            Download
          </button>
        </div>
      </div>

      {/* License class selector (only for DRIVERS_LICENSE) */}
      {isLicense && (
        <div className="mb-4">
          <p className="text-xs font-bold text-text-dark mb-2">License Classification</p>
          <div className="flex gap-2">
            {Object.entries(LICENSE_CLASS_LABELS).map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setSelectedLicenseClass(value)}
                className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold transition-all ${
                  selectedLicenseClass === value
                    ? 'bg-driver-blue text-white shadow-sm'
                    : 'border-2 border-gray-200 text-text-gray hover:border-driver-blue'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Reject form (toggle) */}
      {showRejectForm && (
        <div className="mb-4">
          <textarea
            value={rejectionReason}
            onChange={handleReasonChange}
            placeholder="Provide a reason for rejection (e.g., document is blurry, expired, etc.)"
            className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-300 focus:border-red-400 resize-none"
            rows={3}
            maxLength={REJECTION_REASON.MAX_LENGTH}
          />
          <p className="text-xs text-text-gray mt-1 text-right">{rejectionReason.length}/{REJECTION_REASON.MAX_LENGTH}</p>
        </div>
      )}

      {/* Action buttons */}
      <div className="flex gap-3">
        {!showRejectForm ? (
          <>
            <button
              onClick={handleApprove}
              disabled={isSubmitting}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-green-600 text-white text-sm font-bold rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
            >
              {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle size={16} />}
              Approve
            </button>
            <button
              onClick={() => setShowRejectForm(true)}
              disabled={isSubmitting}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 border-2 border-red-400 text-red-500 text-sm font-bold rounded-lg hover:bg-red-50 transition-colors disabled:opacity-50"
            >
              <XCircle size={16} />
              Reject
            </button>
          </>
        ) : (
          <>
            <button
              onClick={handleReject}
              disabled={isSubmitting || !rejectionReason.trim()}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-red-500 text-white text-sm font-bold rounded-lg hover:bg-red-600 transition-colors disabled:opacity-50"
            >
              {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <XCircle size={16} />}
              Confirm Rejection
            </button>
            <button
              onClick={() => { setShowRejectForm(false); setRejectionReason(''); }}
              disabled={isSubmitting}
              className="px-4 py-2.5 border-2 border-gray-300 text-text-gray text-sm font-semibold rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
          </>
        )}
      </div>

      {showPreview && (
        <FilePreviewModal
          fileUrl={getAdminDocumentViewUrl(document.documentId)}
          fileName={document.fileName}
          onClose={() => setShowPreview(false)}
        />
      )}
    </div>
  );
}
