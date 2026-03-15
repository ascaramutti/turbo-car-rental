import { useState, useEffect } from 'react';
import { X, Loader2, FileText, CheckCircle, XCircle, Clock, Eye, Download } from 'lucide-react';
import { getDocumentsByUser, getAdminDocumentViewUrl, getAdminDocumentDownloadUrl } from '../api/adminDocumentApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';
import FilePreviewModal from '../../../shared/components/common/FilePreviewModal';
import { extractErrorMessage } from '../../auth/utils/validation';
import { DOCUMENT_LABELS, DOCUMENT_STATUS, STATUS_CONFIG } from '../../documents/constants/documentConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';
import toast from 'react-hot-toast';

const STATUS_ICONS = {
  [DOCUMENT_STATUS.PENDING]: Clock,
  [DOCUMENT_STATUS.APPROVED]: CheckCircle,
  [DOCUMENT_STATUS.REJECTED]: XCircle,
};

/**
 * Modal that shows all documents for a specific user.
 * @param {number} userId
 * @param {string} userFullName
 * @param {Function} onClose
 */
export default function UserDocumentsModal({ userId, userFullName, onClose }) {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [previewDoc, setPreviewDoc] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);

  /** Downloads a document file via authenticated API call. */
  const handleDownload = async (doc) => {
    setDownloadingId(doc.documentId);
    try {
      await downloadFile(getAdminDocumentDownloadUrl(doc.documentId), doc.fileName);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setDownloadingId(null);
    }
  };

  useEffect(() => {
    const fetchDocs = async () => {
      try {
        const { data } = await getDocumentsByUser(userId);
        setDocuments(data);
      } catch (err) {
        toast.error(extractErrorMessage(err));
      } finally {
        setIsLoading(false);
      }
    };
    fetchDocs();
  }, [userId]);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[80vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b">
          <div>
            <h2 className="text-lg font-bold text-text-dark">{userFullName}</h2>
            <p className="text-xs text-text-gray">User ID: {userId} — All documents</p>
          </div>
          <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors">
            <X size={20} className="text-text-gray" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 overflow-y-auto max-h-[60vh]">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 size={24} className="animate-spin text-accent-orange" />
            </div>
          ) : documents.length === 0 ? (
            <p className="text-center text-text-gray py-8">No documents uploaded yet.</p>
          ) : (
            <div className="space-y-3">
              {documents.map((doc) => {
                const config = STATUS_CONFIG[doc.status];
                const StatusIcon = STATUS_ICONS[doc.status];
                return (
                  <div key={doc.documentId} className={`border-2 rounded-lg p-4 ${config.border} ${config.bg}`}>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <FileText size={16} className="text-text-gray" />
                        <span className="text-sm font-semibold text-text-dark">
                          {DOCUMENT_LABELS[doc.documentType]}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <StatusIcon size={16} className={config.color} />
                        <span className={`text-xs font-semibold ${config.color}`}>{config.label}</span>
                      </div>
                    </div>
                    <div className="mt-2 flex items-center justify-between">
                      <div className="text-xs text-text-gray">
                        <p>File: {doc.fileName}</p>
                        <p>Uploaded: {formatDateTime(doc.uploadedAt)}</p>
                        {doc.reviewedAt && <p>Reviewed: {formatDateTime(doc.reviewedAt)}</p>}
                        {doc.rejectionReason && (
                          <p className="mt-1 text-red-600">Reason: {doc.rejectionReason}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => setPreviewDoc(doc)}
                          className="inline-flex items-center gap-1 px-2 py-1 text-driver-blue text-xs font-semibold hover:underline"
                        >
                          <Eye size={12} />
                          View
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDownload(doc)}
                          disabled={downloadingId === doc.documentId}
                          className="inline-flex items-center gap-1 px-2 py-1 text-text-gray text-xs font-semibold hover:underline disabled:opacity-50"
                        >
                          {downloadingId === doc.documentId ? <Loader2 size={12} className="animate-spin" /> : <Download size={12} />}
                          Download
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
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
