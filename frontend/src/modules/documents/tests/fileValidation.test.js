import { describe, it, expect } from 'vitest';
import { validateDocumentFile } from '../utils/fileValidation';

describe('validateDocumentFile', () => {
  it('returns error for null file', () => {
    expect(validateDocumentFile(null)).toBe('Please select a file');
  });

  it('returns error for unsupported MIME type (.docx)', () => {
    const file = new File(['content'], 'doc.docx', { type: 'application/msword' });
    expect(validateDocumentFile(file)).toBe('Invalid file format. Only PDF, JPG, and PNG are allowed');
  });

  it('returns error for file exceeding 5MB', () => {
    const file = new File([new ArrayBuffer(6 * 1024 * 1024)], 'large.pdf', { type: 'application/pdf' });
    expect(validateDocumentFile(file)).toContain('exceeds');
  });

  it('returns null for valid PDF under 5MB', () => {
    const file = new File(['pdf-content'], 'license.pdf', { type: 'application/pdf' });
    expect(validateDocumentFile(file)).toBeNull();
  });

  it('returns null for valid JPEG', () => {
    const file = new File(['jpg-content'], 'photo.jpg', { type: 'image/jpeg' });
    expect(validateDocumentFile(file)).toBeNull();
  });

  it('returns null for valid PNG', () => {
    const file = new File(['png-content'], 'scan.png', { type: 'image/png' });
    expect(validateDocumentFile(file)).toBeNull();
  });

  it('returns error for empty MIME type', () => {
    const file = new File(['content'], 'unknown', { type: '' });
    expect(validateDocumentFile(file)).toBe('Invalid file format. Only PDF, JPG, and PNG are allowed');
  });
});
