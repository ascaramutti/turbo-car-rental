import api from '../../../shared/api/axios';

/** Base API path for driver document endpoints. */
const BASE_PATH = '/driver/documents';

/** Headers required for multipart file uploads. */
const MULTIPART_HEADERS = { 'Content-Type': 'multipart/form-data' };

/** Form-data field name for the uploaded file. */
const FIELD_FILE = 'file';

/** Form-data field name for the document type. */
const FIELD_DOCUMENT_TYPE = 'documentType';

/**
 * Uploads a document for the authenticated driver.
 * @param {File} file - PDF, JPG, or PNG file (max 5MB)
 * @param {string} documentType - DRIVERS_LICENSE or STUDY_PERMIT
 */
export function uploadDocument(file, documentType) {
  const formData = new FormData();
  formData.append(FIELD_FILE, file);
  formData.append(FIELD_DOCUMENT_TYPE, documentType);
  return api.post(`${BASE_PATH}/upload`, formData, {
    headers: MULTIPART_HEADERS,
  });
}

/** Returns all documents for the authenticated driver. */
export function getMyDocuments() {
  return api.get(`${BASE_PATH}/my`);
}

/**
 * Returns the API path for viewing a document inline (PDF/image preview).
 * @param {number} documentId
 */
export function getDocumentViewUrl(documentId) {
  return `${BASE_PATH}/${documentId}/view`;
}

/**
 * Returns the API path for downloading a document as attachment.
 * @param {number} documentId
 */
export function getDocumentDownloadUrl(documentId) {
  return `${BASE_PATH}/${documentId}/download`;
}

/**
 * Re-uploads a pending or rejected document.
 * @param {number} documentId
 * @param {File} file - New file to replace the existing one
 */
export function reuploadDocument(documentId, file) {
  const formData = new FormData();
  formData.append(FIELD_FILE, file);
  return api.put(`${BASE_PATH}/${documentId}/reupload`, formData, {
    headers: MULTIPART_HEADERS,
  });
}
