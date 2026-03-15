/**
 * Admin module constants matching the backend API contract (Module 2).
 */

/** Actions an admin can take when reviewing a document. */
export const REVIEW_ACTIONS = {
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
};

/** Available license class values assigned upon document approval. */
export const LICENSE_CLASSES = {
  CLASS_4: 'CLASS_4',
  CLASS_5: 'CLASS_5',
};

/** Human-readable labels for each license class. */
export const LICENSE_CLASS_LABELS = {
  [LICENSE_CLASSES.CLASS_4]: 'Class 4 — Taxi + Delivery',
  [LICENSE_CLASSES.CLASS_5]: 'Class 5 — Delivery Only',
};

/** Rejection reason constraints matching backend ReviewDocumentRequest validation. */
export const REJECTION_REASON = {
  MAX_LENGTH: 500,
  PATTERN: /^[a-zA-Z0-9À-ÿ\s.,!?()'+:]*$/,
  REQUIRED_MESSAGE: 'Please provide a rejection reason',
  PATTERN_MESSAGE: 'Only letters, numbers, spaces, and basic punctuation are allowed (. , ! ? ( ) \' + :)',
};
