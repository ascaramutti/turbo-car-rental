/**
 * Document module constants matching the backend API contract (Module 2).
 */

/** Supported document type identifiers. */
export const DOCUMENT_TYPES = {
  DRIVERS_LICENSE: 'DRIVERS_LICENSE',
  STUDY_PERMIT: 'STUDY_PERMIT',
};

/** Possible review statuses for a submitted document. */
export const DOCUMENT_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

/** Document types that every driver must upload to become verified. */
export const REQUIRED_DOCUMENT_TYPES = [
  DOCUMENT_TYPES.DRIVERS_LICENSE,
  DOCUMENT_TYPES.STUDY_PERMIT,
];

/** Human-readable labels for each document type. */
export const DOCUMENT_LABELS = {
  [DOCUMENT_TYPES.DRIVERS_LICENSE]: "Driver's License",
  [DOCUMENT_TYPES.STUDY_PERMIT]: 'Study Permit',
};

/** File size and format constraints for document uploads. */
export const FILE_CONSTRAINTS = {
  MAX_SIZE_MB: 5,
  MAX_SIZE_BYTES: 5 * 1024 * 1024,
  ACCEPTED_FORMATS: '.pdf,.jpg,.jpeg,.png',
  ACCEPTED_MIME_TYPES: ['application/pdf', 'image/jpeg', 'image/png'],
};

/** UI styling configuration for each document status. */
export const STATUS_CONFIG = {
  [DOCUMENT_STATUS.PENDING]: { color: 'text-amber-500', bg: 'bg-amber-50', border: 'border-amber-200', label: 'Under Review' },
  [DOCUMENT_STATUS.APPROVED]: { color: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', label: 'Approved' },
  [DOCUMENT_STATUS.REJECTED]: { color: 'text-red-500', bg: 'bg-red-50', border: 'border-red-200', label: 'Rejected' },
};
