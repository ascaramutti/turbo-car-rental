import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import DocumentCard from '../components/DocumentCard';
import * as documentApi from '../api/documentApi';
import { downloadFile } from '../../../shared/utils/downloadUtils';

vi.mock('../api/documentApi');
vi.mock('../../../shared/utils/downloadUtils');
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));

const PENDING_LICENSE = { documentId: 1, userId: 2, documentType: 'DRIVERS_LICENSE', fileName: 'license.pdf', fileSize: 2048, status: 'PENDING', uploadedAt: '2026-03-14T10:00:00', reviewedAt: null, rejectionReason: null };
const APPROVED_LICENSE = { ...PENDING_LICENSE, status: 'APPROVED', reviewedAt: '2026-03-14T11:00:00' };
const REJECTED_LICENSE = { ...PENDING_LICENSE, status: 'REJECTED', reviewedAt: '2026-03-14T11:00:00', rejectionReason: 'Document is blurry' };

describe('DocumentCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    URL.createObjectURL = vi.fn(() => 'blob:http://localhost/fake');
    URL.revokeObjectURL = vi.fn();
  });

  it('shows upload card with "Select File" when no document exists', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Select File')).toBeInTheDocument();
    expect(screen.getByText("Driver's License")).toBeInTheDocument();
  });

  it('shows accepted formats text (PDF, JPG, PNG and max size)', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={vi.fn()} />
    );

    expect(screen.getByText(/PDF, JPG, or PNG/)).toBeInTheDocument();
    expect(screen.getByText(/max 5MB/)).toBeInTheDocument();
  });

  it('shows status "Under Review" for PENDING document', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Under Review')).toBeInTheDocument();
  });

  it('shows status "Approved" for APPROVED document', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={APPROVED_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('shows status "Rejected" with rejection reason for REJECTED document', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={REJECTED_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Rejected')).toBeInTheDocument();
    expect(screen.getByText(/Document is blurry/)).toBeInTheDocument();
  });

  it('shows "Replace File" button for PENDING documents', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Replace File')).toBeInTheDocument();
  });

  it('shows "Replace File" button for REJECTED documents', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={REJECTED_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('Replace File')).toBeInTheDocument();
  });

  it('does NOT show "Replace File" for APPROVED documents', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={APPROVED_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.queryByText('Replace File')).not.toBeInTheDocument();
  });

  it('shows "View Document" and "Download" buttons for uploaded documents', () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getByText('View Document')).toBeInTheDocument();
    expect(screen.getByText('Download')).toBeInTheDocument();
  });

  it('calls onUpdate after successful upload', async () => {
    const onUpdate = vi.fn();
    documentApi.uploadDocument.mockResolvedValue({ data: PENDING_LICENSE });

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={onUpdate} />
    );

    const file = new File(['dummy'], 'license.pdf', { type: 'application/pdf' });
    const input = document.querySelector('input[type="file"]');

    await userEvent.upload(input, file);

    await waitFor(() => {
      expect(onUpdate).toHaveBeenCalled();
    });
  });

  it('calls downloadFile when Download button is clicked', async () => {
    downloadFile.mockResolvedValue();

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    await userEvent.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(downloadFile).toHaveBeenCalledWith(
        documentApi.getDocumentDownloadUrl(PENDING_LICENSE.documentId),
        PENDING_LICENSE.fileName
      );
    });
  });

  it('shows error toast on download failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    downloadFile.mockRejectedValue({ response: { data: { message: 'Download failed' } } });

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    await userEvent.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Download failed');
    });
  });

  it('shows "Downloading..." text and spinner during download', async () => {
    let resolveDownload;
    downloadFile.mockImplementation(() => new Promise((resolve) => { resolveDownload = resolve; }));

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    await userEvent.click(screen.getByText('Download'));

    await waitFor(() => {
      expect(screen.getByText('Downloading...')).toBeInTheDocument();
    });

    resolveDownload();

    await waitFor(() => {
      expect(screen.getByText('Download')).toBeInTheDocument();
    });
  });

  it('shows error toast when selecting a file with invalid format (.docx)', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={vi.fn()} />
    );

    const invalidFile = new File(['dummy'], 'resume.docx', { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [invalidFile] } });

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Invalid file format. Only PDF, JPG, and PNG are allowed');
    });
  });

  it('shows error toast when selecting an oversized file', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={vi.fn()} />
    );

    const largeContent = new Uint8Array(6 * 1024 * 1024);
    const oversizedFile = new File([largeContent], 'large.pdf', { type: 'application/pdf' });
    const input = document.querySelector('input[type="file"]');

    await userEvent.upload(input, oversizedFile);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('File size exceeds 5MB limit');
    });
  });

  it('opens FilePreviewModal when "View Document" is clicked', async () => {
    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={vi.fn()} />
    );

    expect(screen.getAllByText(PENDING_LICENSE.fileName)).toHaveLength(1);

    await userEvent.click(screen.getByText('View Document'));

    await waitFor(() => {
      expect(screen.getAllByText(PENDING_LICENSE.fileName)).toHaveLength(2);
    });
  });

  it('calls reuploadDocument when Replace File is used for a PENDING document', async () => {
    const onUpdate = vi.fn();
    documentApi.reuploadDocument.mockResolvedValue({ data: PENDING_LICENSE });

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={onUpdate} />
    );

    const file = new File(['dummy'], 'new-license.pdf', { type: 'application/pdf' });
    const inputs = document.querySelectorAll('input[type="file"]');
    const replaceInput = inputs[inputs.length - 1];

    await userEvent.upload(replaceInput, file);

    await waitFor(() => {
      expect(documentApi.reuploadDocument).toHaveBeenCalledWith(PENDING_LICENSE.documentId, file);
      expect(onUpdate).toHaveBeenCalled();
    });
  });

  it('handleUpload shows error toast on API failure', async () => {
    const toastModule = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toastModule.default, 'error');
    const onUpdate = vi.fn();
    documentApi.uploadDocument.mockRejectedValue({ response: { data: { message: 'Upload failed' } } });

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={null} onUpdate={onUpdate} />
    );

    const file = new File(['dummy'], 'license.pdf', { type: 'application/pdf' });
    const input = document.querySelector('input[type="file"]');

    await userEvent.upload(input, file);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Upload failed');
    });
    expect(onUpdate).not.toHaveBeenCalled();
  });

  it('handleReupload shows error toast on API failure', async () => {
    const toastModule = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toastModule.default, 'error');
    const onUpdate = vi.fn();
    documentApi.reuploadDocument.mockRejectedValue({ response: { data: { message: 'Reupload failed' } } });

    renderWithProviders(
      <DocumentCard documentType="DRIVERS_LICENSE" document={PENDING_LICENSE} onUpdate={onUpdate} />
    );

    const file = new File(['dummy'], 'new-license.pdf', { type: 'application/pdf' });
    const inputs = document.querySelectorAll('input[type="file"]');
    const replaceInput = inputs[inputs.length - 1];

    await userEvent.upload(replaceInput, file);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Reupload failed');
    });
    expect(onUpdate).not.toHaveBeenCalled();
  });
});
