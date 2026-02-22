import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import DriverDocumentsPage from '../pages/DriverDocumentsPage';
import * as documentApi from '../api/documentApi';

vi.mock('../api/documentApi');
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));

const PENDING_LICENSE = { documentId: 1, userId: 2, documentType: 'DRIVERS_LICENSE', fileName: 'license.pdf', fileSize: 2048, status: 'PENDING', uploadedAt: '2026-03-14T10:00:00', reviewedAt: null, rejectionReason: null };
const PENDING_PERMIT = { documentId: 2, userId: 2, documentType: 'STUDY_PERMIT', fileName: 'permit.jpg', fileSize: 1024, status: 'PENDING', uploadedAt: '2026-03-14T10:05:00', reviewedAt: null, rejectionReason: null };
const APPROVED_LICENSE = { ...PENDING_LICENSE, status: 'APPROVED', reviewedAt: '2026-03-14T11:00:00' };
const APPROVED_PERMIT = { ...PENDING_PERMIT, status: 'APPROVED', reviewedAt: '2026-03-14T11:05:00' };
const REJECTED_LICENSE = { ...PENDING_LICENSE, status: 'REJECTED', reviewedAt: '2026-03-14T11:00:00', rejectionReason: 'Document is blurry' };

describe('DriverDocumentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page with title and description', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Document Verification')).toBeInTheDocument();
    });
  });

  it('shows upload cards for both types when no docs exist', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText("Driver's License")).toBeInTheDocument();
      expect(screen.getByText('Study Permit')).toBeInTheDocument();
      expect(screen.getAllByText('Select File')).toHaveLength(2);
    });
  });

  it('shows verification required banner with upload prompt when no docs', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Verification Required')).toBeInTheDocument();
      expect(screen.getByText(/upload your documents below/i)).toBeInTheDocument();
    });
  });

  it('shows verification required with pending count when docs are pending', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [PENDING_LICENSE, PENDING_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Verification Required')).toBeInTheDocument();
      expect(screen.getByText(/2 document\(s\) still need approval/)).toBeInTheDocument();
    });
  });

  it('shows account verified banner when all docs approved', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [APPROVED_LICENSE, APPROVED_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Account Verified')).toBeInTheDocument();
    });
  });

  it('shows pending status for uploaded documents', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [PENDING_LICENSE, PENDING_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Under Review')).toHaveLength(2);
    });
  });

  it('shows rejection reason when document is rejected', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [REJECTED_LICENSE] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Rejected')).toBeInTheDocument();
      expect(screen.getByText(/Document is blurry/)).toBeInTheDocument();
    });
  });

  it('shows replace file button for pending and rejected docs only', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [PENDING_LICENSE, APPROVED_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Replace File')).toHaveLength(1);
    });
  });

  it('shows view document link for uploaded documents', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('View Document')).toBeInTheDocument();
    });
  });

  it('does not show replace file for approved docs', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [APPROVED_LICENSE, APPROVED_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.queryByText('Replace File')).not.toBeInTheDocument();
    });
  });

  it('shows mixed state correctly - one approved, one pending', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [APPROVED_LICENSE, PENDING_PERMIT] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Verification Required')).toBeInTheDocument();
      expect(screen.getByText(/1 document\(s\) still need approval/)).toBeInTheDocument();
      expect(screen.getByText('Approved')).toBeInTheDocument();
      expect(screen.getByText('Under Review')).toBeInTheDocument();
    });
  });

  it('shows upload card for missing doc and status for uploaded one', async () => {
    documentApi.getMyDocuments.mockResolvedValue({ data: [PENDING_LICENSE] });
    renderWithProviders(<DriverDocumentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Under Review')).toBeInTheDocument();
      expect(screen.getByText('Select File')).toBeInTheDocument();
    });
  });
});
