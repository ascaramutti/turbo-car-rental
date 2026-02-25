import api from '../../../shared/api/axios';

/** Base API path for admin document endpoints. */
const BASE_PATH = '/admin/documents';

/** Returns all documents with PENDING status for admin review. */
export function getPendingDocuments() {
  return api.get(`${BASE_PATH}/pending`);
}

/**
 * Returns all documents for a specific user.
 * @param {number} userId
 */
export function getDocumentsByUser(userId) {
  return api.get(`${BASE_PATH}/user/${userId}`);
}

/**
 * Returns the API path for viewing a document inline as admin.
 * @param {number} documentId
 */
export function getAdminDocumentViewUrl(documentId) {
  return `${BASE_PATH}/${documentId}/view`;
}

/**
 * Returns the API path for downloading a document as admin.
 * @param {number} documentId
 */
export function getAdminDocumentDownloadUrl(documentId) {
  return `${BASE_PATH}/${documentId}/download`;
}

/**
 * Approves or rejects a document.
 * @param {number} documentId
 * @param {Object} reviewData - { action, licenseClass?, rejectionReason? }
 */
export function reviewDocument(documentId, reviewData) {
  return api.put(`${BASE_PATH}/${documentId}/review`, reviewData);
}
