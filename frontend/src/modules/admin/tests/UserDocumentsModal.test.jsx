import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import UserDocumentsModal from '../components/UserDocumentsModal';
import * as adminDocumentApi from '../api/adminDocumentApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';
import toast from 'react-hot-toast';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock('../api/adminDocumentApi');
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));
vi.mock('../../../shared/utils/downloadUtils');

const PENDING_DOC = {
  documentId: 1, userId: 5, userFullName: 'John Doe', userRole: 'DRIVER',
  documentType: 'DRIVERS_LICENSE', fileName: 'license.pdf', fileSize: 2048,
  status: 'PENDING', uploadedAt: '2026-03-14T10:00:00', reviewedAt: null,
  reviewedBy: null, rejectionReason: null,
};

const APPROVED_DOC = {
  documentId: 2, userId: 5, userFullName: 'John Doe', userRole: 'DRIVER',
  documentType: 'STUDY_PERMIT', fileName: 'permit.jpg', fileSize: 1024,
  status: 'APPROVED', uploadedAt: '2026-03-14T10:05:00', reviewedAt: '2026-03-15T14:30:00',
  reviewedBy: 'admin', rejectionReason: null,
};

const REJECTED_DOC = {
  documentId: 3, userId: 5, userFullName: 'John Doe', userRole: 'DRIVER',
  documentType: 'DRIVERS_LICENSE', fileName: 'bad_license.pdf', fileSize: 3072,
  status: 'REJECTED', uploadedAt: '2026-03-13T09:00:00', reviewedAt: '2026-03-14T11:00:00',
  reviewedBy: 'admin', rejectionReason: 'Image is blurry and unreadable',
};

const DEFAULT_PROPS = {
  userId: 5,
  userFullName: 'John Doe',
  onClose: vi.fn(),
};

describe('UserDocumentsModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it('shows loading spinner while fetching', () => {
    adminDocumentApi.getDocumentsByUser.mockReturnValue(new Promise(() => {}));
    const { container } = renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    expect(container.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('shows user name and user ID in header', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText(/User ID: 5/)).toBeInTheDocument();
    });
  });

  it('shows "No documents uploaded yet." when empty', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('No documents uploaded yet.')).toBeInTheDocument();
    });
  });

  it('renders documents with correct status labels for PENDING', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [PENDING_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('Under Review')).toBeInTheDocument();
    });
  });

  it('renders documents with correct status labels for APPROVED', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [APPROVED_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('Approved')).toBeInTheDocument();
    });
  });

  it('renders documents with correct status labels for REJECTED', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [REJECTED_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('Rejected')).toBeInTheDocument();
    });
  });

  it('shows rejection reason for rejected documents', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [REJECTED_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText(/Image is blurry and unreadable/)).toBeInTheDocument();
    });
  });

  it('shows View and Download buttons for each document', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [PENDING_DOC, APPROVED_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getAllByText('View')).toHaveLength(2);
      expect(screen.getAllByText('Download')).toHaveLength(2);
    });
  });

  it('calls onClose when X is clicked', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [] });
    const onClose = vi.fn();
    const user = userEvent.setup();

    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} onClose={onClose} />);

    await waitFor(() => {
      expect(screen.getByText('No documents uploaded yet.')).toBeInTheDocument();
    });

    const closeButton = screen.getByRole('button');
    await user.click(closeButton);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('shows reviewed date when document has been reviewed', async () => {
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [APPROVED_DOC] });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText(/Reviewed:/)).toBeInTheDocument();
    });
  });

  it('shows error toast when fetch fails', async () => {
    adminDocumentApi.getDocumentsByUser.mockRejectedValue({ response: { data: { message: 'Failed to load documents' } } });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Failed to load documents');
    });
  });

  it('handleDownload shows error toast on failure', async () => {
    const user = userEvent.setup();
    adminDocumentApi.getDocumentsByUser.mockResolvedValue({ data: [PENDING_DOC] });
    downloadFile.mockRejectedValue({ response: { data: { message: 'Download failed' } } });
    renderWithProviders(<UserDocumentsModal {...DEFAULT_PROPS} />);

    await waitFor(() => {
      expect(screen.getByText('Download')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Download failed');
    });
  });
});
