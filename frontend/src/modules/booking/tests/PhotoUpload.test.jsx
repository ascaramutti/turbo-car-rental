import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import PhotoUpload from '../components/PhotoUpload';

describe('PhotoUpload', () => {
  const mockOnFilesChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Rendering ────────────────────────────────────────────────────────────

  it('renders the upload button when no files are selected', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    expect(screen.getByText(/Choose photos/i)).toBeInTheDocument();
  });

  it('renders the label text', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    expect(screen.getByText('Pickup Photo')).toBeInTheDocument();
  });

  it('renders the file input element', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    const input = document.querySelector('input[type="file"]');
    expect(input).toBeInTheDocument();
  });

  it('renders the file input with the multiple attribute', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    const input = document.querySelector('input[type="file"]');
    expect(input).toHaveAttribute('multiple');
  });

  it('accepts JPG and PNG file formats (no PDF)', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    const input = document.querySelector('input[type="file"]');
    expect(input).toHaveAttribute('accept', '.jpg,.jpeg,.png');
  });

  it('shows max size label in the upload button text', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );
    expect(screen.getByText(/max 5MB/i)).toBeInTheDocument();
  });

  it('shows error message when error prop is provided', () => {
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={[]}
        onFilesChange={mockOnFilesChange}
        error="At least one pickup photo is required"
      />
    );
    expect(screen.getByText('At least one pickup photo is required')).toBeInTheDocument();
  });

  // ── File count indicator ─────────────────────────────────────────────────

  it('shows file count indicator when files are selected', () => {
    const file = new File(['content'], 'pickup.jpg', { type: 'image/jpeg' });
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={[file]}
        onFilesChange={mockOnFilesChange}
        error={null}
      />
    );
    expect(screen.getByText('1/10 photos selected')).toBeInTheDocument();
  });

  it('shows correct count with multiple files', () => {
    const files = [
      new File(['a'], 'a.jpg', { type: 'image/jpeg' }),
      new File(['b'], 'b.jpg', { type: 'image/jpeg' }),
      new File(['c'], 'c.jpg', { type: 'image/jpeg' }),
    ];
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={files}
        onFilesChange={mockOnFilesChange}
        error={null}
      />
    );
    expect(screen.getByText('3/10 photos selected')).toBeInTheDocument();
  });

  // ── File display and removal ─────────────────────────────────────────────

  it('shows the selected file name when files are provided', () => {
    const file = new File(['content'], 'pickup.jpg', { type: 'image/jpeg' });
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[file]} onFilesChange={mockOnFilesChange} error={null} />
    );
    expect(screen.getByText('pickup.jpg')).toBeInTheDocument();
  });

  it('shows a remove button for each selected file', () => {
    const files = [
      new File(['a'], 'a.jpg', { type: 'image/jpeg' }),
      new File(['b'], 'b.png', { type: 'image/png' }),
    ];
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={files} onFilesChange={mockOnFilesChange} error={null} />
    );
    expect(screen.getByRole('button', { name: /Remove a\.jpg/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Remove b\.png/i })).toBeInTheDocument();
  });

  it('calls onFilesChange without the removed file when a remove button is clicked', async () => {
    const user = userEvent.setup();
    const fileA = new File(['a'], 'a.jpg', { type: 'image/jpeg' });
    const fileB = new File(['b'], 'b.jpg', { type: 'image/jpeg' });
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={[fileA, fileB]}
        onFilesChange={mockOnFilesChange}
        error={null}
      />
    );

    await user.click(screen.getByRole('button', { name: /Remove a\.jpg/i }));

    expect(mockOnFilesChange).toHaveBeenCalledWith([fileB], null);
  });

  // ── Validation: multiple valid files ─────────────────────────────────────

  it('calls onFilesChange with merged array when valid files are added', async () => {
    const existingFile = new File(['existing'], 'existing.jpg', { type: 'image/jpeg' });
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={[existingFile]}
        onFilesChange={mockOnFilesChange}
        error={null}
      />
    );

    const newFile = new File(['new'], 'new.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [newFile] } });

    expect(mockOnFilesChange).toHaveBeenCalledWith([existingFile, newFile], null);
  });

  // ── Validation: file too large ────────────────────────────────────────────

  it('calls onFilesChange with original files and error when a file exceeds 5MB', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );

    const largeContent = new Uint8Array(6 * 1024 * 1024); // 6MB
    const oversizedFile = new File([largeContent], 'large.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [oversizedFile] } });

    expect(mockOnFilesChange).toHaveBeenCalledWith([], 'File size must not exceed 5MB');
  });

  // ── Validation: non-image file ────────────────────────────────────────────

  it('calls onFilesChange with original files and error when a non-image file is selected', () => {
    renderWithProviders(
      <PhotoUpload label="Pickup Photo" files={[]} onFilesChange={mockOnFilesChange} error={null} />
    );

    const pdfFile = new File(['pdf content'], 'document.pdf', { type: 'application/pdf' });
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [pdfFile] } });

    expect(mockOnFilesChange).toHaveBeenCalledWith([], 'Only JPG and PNG files are accepted');
  });

  // ── Validation: max files exceeded ───────────────────────────────────────

  it('calls onFilesChange with original files and error when adding files exceeds maxFiles', () => {
    // Already have 9 files; adding 2 more would exceed 10
    const existingFiles = Array.from({ length: 9 }, (_, i) =>
      new File([`content${i}`], `file${i}.jpg`, { type: 'image/jpeg' })
    );
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={existingFiles}
        onFilesChange={mockOnFilesChange}
        error={null}
        maxFiles={10}
      />
    );

    const extraFile1 = new File(['extra1'], 'extra1.jpg', { type: 'image/jpeg' });
    const extraFile2 = new File(['extra2'], 'extra2.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]');

    fireEvent.change(input, { target: { files: [extraFile1, extraFile2] } });

    expect(mockOnFilesChange).toHaveBeenCalledWith(
      existingFiles,
      'You can upload a maximum of 10 photos'
    );
  });

  it('hides the upload button when maxFiles is reached', () => {
    const files = Array.from({ length: 10 }, (_, i) =>
      new File([`content${i}`], `file${i}.jpg`, { type: 'image/jpeg' })
    );
    renderWithProviders(
      <PhotoUpload
        label="Pickup Photo"
        files={files}
        onFilesChange={mockOnFilesChange}
        error={null}
        maxFiles={10}
      />
    );
    expect(screen.queryByText(/Choose photos/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Add more photos/i)).not.toBeInTheDocument();
  });
});
