import { useState, useEffect, useCallback } from 'react';
import { getPendingDocuments } from '../api/adminDocumentApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import ReviewDocumentCard from '../components/ReviewDocumentCard';
import UserDocumentsModal from '../components/UserDocumentsModal';
import { ClipboardCheck, Loader2, Inbox } from 'lucide-react';
import toast from 'react-hot-toast';

export default function AdminDocumentsPage() {
  const [pendingDocs, setPendingDocs] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);

  /** Fetches all pending documents from the API. */
  const fetchPending = useCallback(async () => {
    try {
      const { data } = await getPendingDocuments();
      setPendingDocs(data);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPending();
  }, [fetchPending]);

  /** Opens the user documents modal. */
  const handleViewUser = (doc) => {
    setSelectedUser({ userId: doc.userId, userFullName: doc.userFullName });
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
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="flex items-center gap-3 mb-8">
          <ClipboardCheck size={28} className="text-driver-blue" />
          <div>
            <h1 className="text-2xl font-bold text-text-dark">Verification Portal</h1>
            <p className="text-text-gray text-sm">
              Review and verify driver documents
            </p>
          </div>
        </div>

        {/* Pending count badge */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-text-dark">Pending Reviews</span>
            <span className="bg-amber-100 text-amber-700 text-xs font-bold px-2.5 py-0.5 rounded-full">
              {pendingDocs.length}
            </span>
          </div>
        </div>

        {/* Document cards or empty state */}
        {pendingDocs.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-xl border-2 border-dashed border-gray-200">
            <Inbox size={48} className="mx-auto text-gray-300 mb-4" />
            <p className="text-lg font-semibold text-text-dark">No pending documents</p>
            <p className="text-sm text-text-gray mt-1">All documents have been reviewed. Check back later.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {pendingDocs.map((doc) => (
              <div key={doc.documentId}>
                <ReviewDocumentCard document={doc} onReviewed={fetchPending} />
                <button
                  onClick={() => handleViewUser(doc)}
                  className="mt-2 text-xs text-driver-blue hover:underline font-medium"
                >
                  View all documents for {doc.userFullName}
                </button>
              </div>
            ))}
          </div>
        )}

        {/* User documents modal */}
        {selectedUser && (
          <UserDocumentsModal
            userId={selectedUser.userId}
            userFullName={selectedUser.userFullName}
            onClose={() => setSelectedUser(null)}
          />
        )}
      </div>
    </div>
  );
}
