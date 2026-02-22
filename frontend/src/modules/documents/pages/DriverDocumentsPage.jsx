import { useState, useEffect, useCallback } from 'react';
import { getMyDocuments } from '../api/documentApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { REQUIRED_DOCUMENT_TYPES, DOCUMENT_STATUS, FILE_CONSTRAINTS } from '../constants/documentConstants';
import DocumentCard from '../components/DocumentCard';
import { ShieldCheck, AlertTriangle, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

export default function DriverDocumentsPage() {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  /** Fetches the driver's documents from the API. */
  const fetchDocuments = useCallback(async () => {
    try {
      const { data } = await getMyDocuments();
      setDocuments(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  /** Finds the document object for a given type, or null if not uploaded. */
  const getDocumentByType = (type) =>
    documents.find((d) => d.documentType === type) || null;

  /** Checks if all required documents are approved. */
  const allApproved = REQUIRED_DOCUMENT_TYPES.every(
    (type) => getDocumentByType(type)?.status === DOCUMENT_STATUS.APPROVED
  );

  /** Counts how many documents are still pending or not uploaded. */
  const pendingCount = REQUIRED_DOCUMENT_TYPES.filter(
    (type) => getDocumentByType(type)?.status !== DOCUMENT_STATUS.APPROVED
  ).length;

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-bg-light px-4 py-8">
      <div className="max-w-2xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-text-dark">Document Verification</h1>
          <p className="text-text-gray mt-1">
            Upload your documents to get verified and start booking vehicles
          </p>
        </div>

        {/* Status Banner */}
        {allApproved ? (
          <div className="flex items-center gap-3 bg-green-50 border-2 border-green-200 rounded-xl p-4 mb-8">
            <ShieldCheck size={24} className="text-green-600" />
            <div>
              <p className="font-bold text-green-700">Account Verified</p>
              <p className="text-sm text-green-600">All documents approved. You can now browse and book vehicles.</p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 bg-amber-50 border-2 border-amber-200 rounded-xl p-4 mb-8">
            <AlertTriangle size={24} className="text-amber-500" />
            <div>
              <p className="font-bold text-amber-700">Verification Required</p>
              <p className="text-sm text-amber-600">
                {documents.length > 0
                  ? `${pendingCount} document(s) still need approval before you can book vehicles.`
                  : 'Upload your documents below to get started.'}
              </p>
            </div>
          </div>
        )}

        {/* Document Cards */}
        <div className="space-y-4">
          {REQUIRED_DOCUMENT_TYPES.map((type) => (
            <DocumentCard
              key={type}
              documentType={type}
              document={getDocumentByType(type)}
              onUpdate={fetchDocuments}
            />
          ))}
        </div>

        {/* Help Text */}
        <div className="mt-8 text-center">
          <p className="text-xs text-text-gray">
            Accepted formats: PDF, JPG, PNG (max {FILE_CONSTRAINTS.MAX_SIZE_MB}MB per file).
            Documents are reviewed by our admin team within 24 hours.
          </p>
        </div>
      </div>
    </div>
  );
}
