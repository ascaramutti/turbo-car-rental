import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import AdminDocumentsPage from '../pages/AdminDocumentsPage';
import * as adminDocumentApi from '../api/adminDocumentApi';
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

const PENDING_LICENSE = {
  documentId: 1, userId: 5, userFullName: 'John Doe', userRole: 'DRIVER',
  documentType: 'DRIVERS_LICENSE', fileName: 'license.pdf', fileSize: 2048,
  status: 'PENDING', uploadedAt: '2026-03-14T10:00:00', reviewedAt: null,
  reviewedBy: null, rejectionReason: null,
};

const PENDING_PERMIT = {
  documentId: 2, userId: 5, userFullName: 'John Doe', userRole: 'DRIVER',
  documentType: 'STUDY_PERMIT', fileName: 'permit.jpg', fileSize: 1024,
  status: 'PENDING', uploadedAt: '2026-03-14T10:05:00', reviewedAt: null,
  reviewedBy: null, rejectionReason: null,
};

describe('AdminDocumentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the verification portal with title', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Verification Portal')).toBeInTheDocument();
    });
  });

  it('shows empty state when no pending documents', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('No pending documents')).toBeInTheDocument();
      expect(screen.getByText(/all documents have been reviewed/i)).toBeInTheDocument();
    });
  });

  it('shows pending count badge', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE, PENDING_PERMIT] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('renders review cards for each pending document', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE, PENDING_PERMIT] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getAllByText('John Doe')).toHaveLength(2);
      expect(screen.getAllByText('Approve')).toHaveLength(2);
      expect(screen.getAllByText('Reject')).toHaveLength(2);
    });
  });

  it('shows document type labels on cards', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText("Driver's License")).toBeInTheDocument();
    });
  });

  it('shows license class selector for DRIVERS_LICENSE', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('License Classification')).toBeInTheDocument();
      expect(screen.getByText(/Class 4/)).toBeInTheDocument();
      expect(screen.getByText(/Class 5/)).toBeInTheDocument();
    });
  });

  it('does not show license class selector for STUDY_PERMIT', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_PERMIT] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.queryByText('License Classification')).not.toBeInTheDocument();
    });
  });

  it('shows view and download buttons for admin to review file', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('View')).toBeInTheDocument();
      expect(screen.getByText('Download')).toBeInTheDocument();
    });
  });

  it('shows view all documents link for each user', async () => {
    adminDocumentApi.getPendingDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText(/view all documents for John Doe/i)).toBeInTheDocument();
    });
  });

  it('shows error toast when fetch fails', async () => {
    adminDocumentApi.getPendingDocuments.mockRejectedValue({ response: { data: { message: 'Failed to load pending documents' } } });
    renderWithProviders(<AdminDocumentsPage />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Failed to load pending documents');
    });
  });
});
