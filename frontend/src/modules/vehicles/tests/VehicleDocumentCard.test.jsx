import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleDocumentCard from '../components/VehicleDocumentCard';
import * as vehicleApi from '../api/vehicleApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';

vi.mock('../api/vehicleApi');
vi.mock('../../../shared/utils/downloadUtils');
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));

const PENDING_DOC = {
  documentId: 10, vehicleId: 100, documentType: 'INSURANCE',
  fileName: 'insurance.pdf', fileSize: 2048, status: 'PENDING',
  uploadedAt: '2026-03-14T10:00:00', reviewedAt: null, rejectionReason: null,
};

const APPROVED_DOC = { ...PENDING_DOC, status: 'APPROVED', reviewedAt: '2026-03-14T11:00:00' };
const REJECTED_DOC = { ...PENDING_DOC, status: 'REJECTED', reviewedAt: '2026-03-14T11:00:00', rejectionReason: 'Document is expired' };

describe('VehicleDocumentCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:http://localhost/fake');
    URL.revokeObjectURL = vi.fn();
  });

  it('shows upload card with "Select File" when no document exists', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={null} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Select File')).toBeInTheDocument();
    expect(screen.getByText('Insurance Certificate')).toBeInTheDocument();
  });

  it('shows "Under Review" status for PENDING document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Under Review')).toBeInTheDocument();
  });

  it('shows "Approved" status for APPROVED document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={APPROVED_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('shows "Rejected" status with rejection reason for REJECTED document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={REJECTED_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Rejected')).toBeInTheDocument();
    expect(screen.getByText(/Document is expired/)).toBeInTheDocument();
  });

  it('shows "Replace File" for PENDING document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Replace File')).toBeInTheDocument();
  });

  it('shows "Replace File" for REJECTED document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={REJECTED_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Replace File')).toBeInTheDocument();
  });

  it('does NOT show "Replace File" for APPROVED document', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={APPROVED_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.queryByText('Replace File')).not.toBeInTheDocument();
  });

  it('shows "View Document" and "Download" buttons for uploaded documents', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('View Document')).toBeInTheDocument();
    expect(screen.getByText('Download')).toBeInTheDocument();
  });

  it('calls downloadFile when Download button is clicked', async () => {
    const user = userEvent.setup();
    downloadFile.mockResolvedValue();

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    await user.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(downloadFile).toHaveBeenCalledWith(
        vehicleApi.getVehicleDocumentDownloadUrl(100, 10),
        'insurance.pdf'
      );
    });
  });

  it('opens FilePreviewModal when "View Document" is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    expect(screen.getAllByText('insurance.pdf')).toHaveLength(1);

    await user.click(screen.getByText('View Document'));

    await waitFor(() => {
      expect(screen.getAllByText('insurance.pdf')).toHaveLength(2);
    });
  });

  it('shows error toast on download failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    const user = userEvent.setup();
    downloadFile.mockRejectedValue({ response: { data: { message: 'Download failed' } } });

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    await user.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Download failed');
    });
  });

  // ── New tests for uncovered lines ──────────────────────────────────────

  it('uploads a file when selecting from the upload card (no document)', async () => {
    const user = userEvent.setup();
    const onUpdate = vi.fn();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.uploadVehicleDocument.mockResolvedValue({ data: {} });

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={null} onUpdate={onUpdate} />
    );

    const fileInput = document.querySelector('input[type="file"]');
    const testFile = new File(['content'], 'insurance.pdf', { type: 'application/pdf' });
    await user.upload(fileInput, testFile);

    await waitFor(() => {
      expect(vehicleApi.uploadVehicleDocument).toHaveBeenCalledWith(100, testFile, 'INSURANCE');
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Insurance Certificate uploaded successfully');
    });

    expect(onUpdate).toHaveBeenCalled();
  });

  it('re-uploads a file when selecting from the Replace File button on REJECTED document', async () => {
    const user = userEvent.setup();
    const onUpdate = vi.fn();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.reuploadVehicleDocument.mockResolvedValue({ data: {} });

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={REJECTED_DOC} onUpdate={onUpdate} />
    );

    // The replace file input is the second file input (first is not present since doc exists)
    const fileInputs = document.querySelectorAll('input[type="file"]');
    const replaceInput = fileInputs[0];
    const testFile = new File(['new-content'], 'insurance-v2.pdf', { type: 'application/pdf' });
    await user.upload(replaceInput, testFile);

    await waitFor(() => {
      expect(vehicleApi.reuploadVehicleDocument).toHaveBeenCalledWith(100, 10, testFile);
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Insurance Certificate re-uploaded successfully');
    });

    expect(onUpdate).toHaveBeenCalled();
  });

  it('shows error toast when upload fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.uploadVehicleDocument.mockRejectedValue({
      response: { data: { message: 'Upload failed' } },
    });

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={null} onUpdate={vi.fn()} />
    );

    const fileInput = document.querySelector('input[type="file"]');
    const testFile = new File(['content'], 'insurance.pdf', { type: 'application/pdf' });
    await user.upload(fileInput, testFile);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Upload failed');
    });
  });

  it('shows error toast when file validation fails (invalid file type)', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={null} onUpdate={vi.fn()} />
    );

    const fileInput = document.querySelector('input[type="file"]');
    const invalidFile = new File(['content'], 'insurance.txt', { type: 'text/plain' });
    // Use fireEvent.change to bypass the accept attribute filtering of userEvent.upload
    fireEvent.change(fileInput, { target: { files: [invalidFile] } });

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Invalid file format. Only PDF, JPG, and PNG are allowed');
    });

    expect(vehicleApi.uploadVehicleDocument).not.toHaveBeenCalled();
  });

  it('shows error toast when reupload fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.reuploadVehicleDocument.mockRejectedValue({
      response: { data: { message: 'Reupload failed' } },
    });

    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    const fileInput = document.querySelector('input[type="file"]');
    const testFile = new File(['content'], 'insurance.pdf', { type: 'application/pdf' });
    await user.upload(fileInput, testFile);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Reupload failed');
    });
  });

  it('closes FilePreviewModal when close button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={PENDING_DOC} onUpdate={vi.fn()} />
    );

    await user.click(screen.getByText('View Document'));

    await waitFor(() => {
      expect(screen.getAllByText('insurance.pdf')).toHaveLength(2);
    });

    // The FilePreviewModal has a header with the filename and a close button.
    // The modal overlay is the outermost div. Click outside the modal content to close.
    // Actually the close button is inside the modal header. Find the X button in the modal.
    const modalOverlay = document.querySelector('.fixed.inset-0');
    // The close button is the button inside the modal header (border-b div)
    const headerDiv = modalOverlay.querySelector('.border-b');
    const closeBtn = headerDiv.querySelector('button');
    await user.click(closeBtn);

    await waitFor(() => {
      expect(screen.getAllByText('insurance.pdf')).toHaveLength(1);
    });
  });

  it('does not call upload when no file is selected (empty selection)', async () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSURANCE" document={null} onUpdate={vi.fn()} />
    );

    const fileInput = document.querySelector('input[type="file"]');
    // Simulate a change event with no files
    const changeEvent = new Event('change', { bubbles: true });
    Object.defineProperty(changeEvent, 'target', {
      value: { files: [], value: '' },
    });
    fileInput.dispatchEvent(changeEvent);

    expect(vehicleApi.uploadVehicleDocument).not.toHaveBeenCalled();
  });

  it('shows correct label for VEHICLE_REGISTRATION document type', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="VEHICLE_REGISTRATION" document={null} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Vehicle Registration')).toBeInTheDocument();
  });

  it('shows correct label for INSPECTION_REPORT document type', () => {
    renderWithProviders(
      <VehicleDocumentCard vehicleId={100} documentType="INSPECTION_REPORT" document={null} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Inspection Report (CVIP)')).toBeInTheDocument();
  });
});
