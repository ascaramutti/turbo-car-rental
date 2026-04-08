import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import ReviewDocumentCard from '../components/ReviewDocumentCard';
import * as adminDocumentApi from '../api/adminDocumentApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';
import toast from 'react-hot-toast';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock('../api/adminDocumentApi', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    reviewDocument: vi.fn(),
  };
});
vi.mock('../api/adminVehicleApi', () => ({
  checkVehicleClassification: vi.fn(),
  approveVehicle: vi.fn(),
}));
vi.mock('../../../shared/utils/downloadUtils');
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

describe('ReviewDocumentCard — rejection reason validation', () => {
  const onReviewed = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('allows valid characters in rejection reason', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    await user.type(textarea, 'Document is blurry, please reupload.');
    expect(textarea).toHaveValue('Document is blurry, please reupload.');
  });

  it('blocks XSS characters like < > /', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    // < > / are blocked, letters and () pass through
    await user.type(textarea, 'test<script>alert(1)</script>');
    expect(textarea).toHaveValue('testscriptalert(1)script');
  });

  it('blocks SQL injection characters like ; and -', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    // ; and - are blocked, spaces between them remain
    await user.type(textarea, "DROP TABLE; -- users");
    expect(textarea).toHaveValue('DROP TABLE  users');
  });

  it('allows accented characters', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    await user.type(textarea, "Documento está borroso, résumé inválido");
    expect(textarea).toHaveValue("Documento está borroso, résumé inválido");
  });

  it('allows basic punctuation: . , ! ? ( ) + :', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    await user.type(textarea, "Reason: blurry (low quality), try again!");
    expect(textarea).toHaveValue("Reason: blurry (low quality), try again!");
  });

  it('shows character counter with max length', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);

    await user.type(textarea, 'Blurry');
    expect(screen.getByText('6/500')).toBeInTheDocument();
  });
});

describe('ReviewDocumentCard — rendering and actions', () => {
  const onReviewed = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders user name and document type label', () => {
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText("Driver's License")).toBeInTheDocument();
  });

  it('shows file name and uploaded date', () => {
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    expect(screen.getByText('license.pdf')).toBeInTheDocument();
    expect(screen.getByText(new Date('2026-03-14T10:00:00').toLocaleString())).toBeInTheDocument();
  });

  it('shows View and Download buttons', () => {
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    expect(screen.getByText('View')).toBeInTheDocument();
    expect(screen.getByText('Download')).toBeInTheDocument();
  });

  it('shows license class selector for DRIVERS_LICENSE type', () => {
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    expect(screen.getByText('License Classification')).toBeInTheDocument();
    expect(screen.getByText('Class 4 — Taxi + Delivery')).toBeInTheDocument();
    expect(screen.getByText('Class 5 — Delivery Only')).toBeInTheDocument();
  });

  it('does NOT show license class selector for STUDY_PERMIT type', () => {
    renderWithProviders(<ReviewDocumentCard document={PENDING_PERMIT} onReviewed={onReviewed} />);

    expect(screen.queryByText('License Classification')).not.toBeInTheDocument();
  });

  it('handleApprove calls reviewDocument with APPROVE action and CLASS_4 by default', async () => {
    const user = userEvent.setup();
    adminDocumentApi.reviewDocument.mockResolvedValue({ data: { vehicleId: null } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Approve'));

    await waitFor(() => {
      expect(adminDocumentApi.reviewDocument).toHaveBeenCalledWith(1, {
        action: 'APPROVE',
        licenseClass: 'CLASS_4',
      });
    });
    await waitFor(() => {
      expect(onReviewed).toHaveBeenCalled();
    });
  });

  it('handleApprove calls reviewDocument with CLASS_5 when selected', async () => {
    const user = userEvent.setup();
    adminDocumentApi.reviewDocument.mockResolvedValue({ data: { vehicleId: null } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Class 5 — Delivery Only'));
    await user.click(screen.getByText('Approve'));

    await waitFor(() => {
      expect(adminDocumentApi.reviewDocument).toHaveBeenCalledWith(1, {
        action: 'APPROVE',
        licenseClass: 'CLASS_5',
      });
    });
    await waitFor(() => {
      expect(onReviewed).toHaveBeenCalled();
    });
  });

  it('handleReject calls reviewDocument with REJECT action and reason', async () => {
    const user = userEvent.setup();
    adminDocumentApi.reviewDocument.mockResolvedValue({ data: { vehicleId: null } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);
    await user.type(textarea, 'Document is expired');
    await user.click(screen.getByText('Confirm Rejection'));

    await waitFor(() => {
      expect(adminDocumentApi.reviewDocument).toHaveBeenCalledWith(1, {
        action: 'REJECT',
        rejectionReason: 'Document is expired',
      });
    });
    await waitFor(() => {
      expect(onReviewed).toHaveBeenCalled();
    });
  });

  it('shows Confirm Rejection and Cancel buttons when reject form is open', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));

    expect(screen.getByText('Confirm Rejection')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('Cancel button clears rejection reason and hides form', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);
    await user.type(textarea, 'Some reason');
    expect(textarea).toHaveValue('Some reason');

    await user.click(screen.getByText('Cancel'));

    expect(screen.queryByPlaceholderText(/provide a reason/i)).not.toBeInTheDocument();
    expect(screen.queryByText('Confirm Rejection')).not.toBeInTheDocument();
    // Approve and Reject buttons are back
    expect(screen.getByText('Approve')).toBeInTheDocument();
    expect(screen.getByText('Reject')).toBeInTheDocument();
  });

  it('handleDownload calls downloadFile utility', async () => {
    const user = userEvent.setup();
    downloadFile.mockResolvedValue(undefined);
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(downloadFile).toHaveBeenCalledWith('/admin/documents/1/download', 'license.pdf');
    });
  });

  it('opens FilePreviewModal when View is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('View'));

    // FilePreviewModal renders the fileName in its header
    // There will be two: one in the card, one in the modal header
    const fileNameElements = screen.getAllByText('license.pdf');
    expect(fileNameElements.length).toBeGreaterThanOrEqual(2);
  });

  it('handleApprove shows error toast on API failure', async () => {
    const user = userEvent.setup();
    adminDocumentApi.reviewDocument.mockRejectedValue({ response: { data: { message: 'Document not pending' } } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Approve'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Document not pending');
    });
    expect(onReviewed).not.toHaveBeenCalled();
  });

  it('handleReject shows error toast on API failure', async () => {
    const user = userEvent.setup();
    adminDocumentApi.reviewDocument.mockRejectedValue({ response: { data: { message: 'Rejection failed' } } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Reject'));
    const textarea = screen.getByPlaceholderText(/provide a reason/i);
    await user.type(textarea, 'Document is expired');
    await user.click(screen.getByText('Confirm Rejection'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Rejection failed');
    });
    expect(onReviewed).not.toHaveBeenCalled();
  });

  it('handleDownload shows error toast on failure', async () => {
    const user = userEvent.setup();
    downloadFile.mockRejectedValue({ response: { data: { message: 'Download failed' } } });
    renderWithProviders(<ReviewDocumentCard document={PENDING_LICENSE} onReviewed={onReviewed} />);

    await user.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Download failed');
    });
  });
});
