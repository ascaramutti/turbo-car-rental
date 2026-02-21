import { FILE_CONSTRAINTS } from '../constants/documentConstants';

/**
 * Validates a file against document upload constraints.
 * @param {File} file
 * @returns {string|null} Error message if invalid, null if valid
 */
export function validateDocumentFile(file) {
  if (!file) return 'Please select a file';

  if (!FILE_CONSTRAINTS.ACCEPTED_MIME_TYPES.includes(file.type)) {
    return 'Invalid file format. Only PDF, JPG, and PNG are allowed';
  }

  if (file.size > FILE_CONSTRAINTS.MAX_SIZE_BYTES) {
    return `File size exceeds ${FILE_CONSTRAINTS.MAX_SIZE_MB}MB limit`;
  }

  return null;
}
