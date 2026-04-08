import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../../test/renderWithProviders';
import FilePreviewModal from '../FilePreviewModal';
import api from '../../../api/axios';

vi.mock('../../../api/axios');
vi.mock('react-pdf', () => ({
  Document: ({ children, onLoadSuccess, loading }) => {
    onLoadSuccess?.({ numPages: 3 });
    return <div data-testid="pdf-document">{children}</div>;
  },
  Page: ({ pageNumber }) => <div data-testid="pdf-page">Page {pageNumber}</div>,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));

const FAKE_BLOB_URL = 'blob:http://localhost:3000/fake-uuid';

describe('FilePreviewModal', () => {
  const defaultProps = {
    fileUrl: '/driver/documents/1/view',
    fileName: 'license.pdf',
    onClose: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    globalThis.URL.createObjectURL = vi.fn(() => FAKE_BLOB_URL);
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it('shows loading state while fetching', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    expect(screen.getByText('Loading document...')).toBeInTheDocument();
  });

  it('shows file name in header', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    expect(screen.getByText('license.pdf')).toBeInTheDocument();
  });

  it('shows error message on fetch failure', async () => {
    api.get.mockRejectedValue(new Error('Network error'));
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Failed to load document. Please try again.')).toBeInTheDocument();
    });
  });

  it('renders image when file type is image/jpeg', async () => {
    const blob = new Blob(['img'], { type: 'image/jpeg' });
    api.get.mockResolvedValue({ data: blob });
    renderWithProviders(<FilePreviewModal {...defaultProps} fileName="photo.jpg" />);

    await waitFor(() => {
      const img = screen.getByAltText('photo.jpg');
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('src', FAKE_BLOB_URL);
    });
  });

  it('renders PDF document via react-pdf', async () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    api.get.mockResolvedValue({ data: blob });
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByTestId('pdf-document')).toBeInTheDocument();
      expect(screen.getByTestId('pdf-page')).toBeInTheDocument();
    });
  });

  it('shows pagination controls for multi-page PDFs', async () => {
    const blob = new Blob(['pdf'], { type: 'application/pdf' });
    api.get.mockResolvedValue({ data: blob });
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('1 / 3')).toBeInTheDocument();
    });
  });

  it('shows download button for unsupported file types', async () => {
    const blob = new Blob(['data'], { type: 'application/zip' });
    api.get.mockResolvedValue({ data: blob });
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Download File')).toBeInTheDocument();
    });
  });

  it('calls onClose when X button is clicked', async () => {
    api.get.mockReturnValue(new Promise(() => {}));
    const user = userEvent.setup();
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    await user.click(screen.getByRole('button'));

    expect(defaultProps.onClose).toHaveBeenCalledOnce();
  });

  it('fetches file with correct URL and responseType blob', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<FilePreviewModal {...defaultProps} />);

    expect(api.get).toHaveBeenCalledWith('/driver/documents/1/view', { responseType: 'blob' });
  });

  it('creates and revokes blob URL on mount and unmount', async () => {
    const blob = new Blob(['img'], { type: 'image/jpeg' });
    api.get.mockResolvedValue({ data: blob });
    const { unmount } = renderWithProviders(<FilePreviewModal {...defaultProps} fileName="photo.jpg" />);

    await waitFor(() => {
      expect(URL.createObjectURL).toHaveBeenCalledWith(blob);
    });

    unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(FAKE_BLOB_URL);
  });
});
