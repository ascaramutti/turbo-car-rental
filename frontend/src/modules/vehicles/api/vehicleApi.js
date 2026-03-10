import api from '../../../shared/api/axios';

/** Base API path for car owner vehicle endpoints. */
const BASE_PATH = '/owner/vehicles';

/** Headers required for multipart file uploads. */
const MULTIPART_HEADERS = { 'Content-Type': 'multipart/form-data' };

/** Form-data field name for the uploaded file. */
const FIELD_FILE = 'file';

/** Form-data field name for the vehicle document type. */
const FIELD_DOCUMENT_TYPE = 'documentType';

// ── Vehicle CRUD ────────────────────────────────────────────────────────

/**
 * Registers a new vehicle for the authenticated car owner.
 * @param {Object} data - Vehicle registration payload
 * @returns {Promise} Axios response with the created vehicle
 */
export function registerVehicle(data) {
  return api.post(BASE_PATH, data);
}

/**
 * Returns all vehicles belonging to the authenticated car owner.
 * @returns {Promise} Axios response with an array of vehicles
 */
export function getMyVehicles() {
  return api.get(BASE_PATH);
}

/**
 * Returns a single vehicle by its ID.
 * @param {number} id - Vehicle ID
 * @returns {Promise} Axios response with the vehicle object
 */
export function getVehicleById(id) {
  return api.get(`${BASE_PATH}/${id}`);
}

/**
 * Updates an existing vehicle.
 * @param {number} id - Vehicle ID
 * @param {Object} data - Updated vehicle data
 * @returns {Promise} Axios response with the updated vehicle
 */
export function updateVehicle(id, data) {
  return api.put(`${BASE_PATH}/${id}`, data);
}

/**
 * Lists the vehicle for rent (only if admin-approved).
 * @param {number} id - Vehicle ID
 * @param {Object} data - Activation payload with availableUntil, generalLocation, latitude, longitude
 * @returns {Promise} Axios response with updated vehicle
 */
export function activateVehicle(id, data) {
  return api.put(`${BASE_PATH}/${id}/activate`, data);
}

/**
 * Removes the vehicle from rent listings.
 * @param {number} id - Vehicle ID
 * @returns {Promise} Axios response
 */
export function deactivateVehicle(id) {
  return api.put(`${BASE_PATH}/${id}/deactivate`);
}

// ── Vehicle Documents ───────────────────────────────────────────────────

/**
 * Uploads a document for a specific vehicle.
 * @param {number} vehicleId - Vehicle ID
 * @param {File} file - PDF, JPG, or PNG file (max 5MB)
 * @param {string} documentType - INSURANCE, VEHICLE_REGISTRATION, or INSPECTION_REPORT
 * @returns {Promise} Axios response with the created document
 */
export function uploadVehicleDocument(vehicleId, file, documentType) {
  const formData = new FormData();
  formData.append(FIELD_FILE, file);
  formData.append(FIELD_DOCUMENT_TYPE, documentType);
  return api.post(`${BASE_PATH}/${vehicleId}/documents/upload`, formData, {
    headers: MULTIPART_HEADERS,
  });
}

/**
 * Returns all documents for a specific vehicle.
 * @param {number} vehicleId - Vehicle ID
 * @returns {Promise} Axios response with an array of vehicle documents
 */
export function getVehicleDocuments(vehicleId) {
  return api.get(`${BASE_PATH}/${vehicleId}/documents`);
}

/**
 * Returns the API path for viewing a vehicle document inline.
 * @param {number} vehicleId - Vehicle ID
 * @param {number} docId - Document ID
 * @returns {string} URL path for inline viewing
 */
export function getVehicleDocumentViewUrl(vehicleId, docId) {
  return `${BASE_PATH}/${vehicleId}/documents/${docId}/view`;
}

/**
 * Returns the API path for downloading a vehicle document as attachment.
 * @param {number} vehicleId - Vehicle ID
 * @param {number} docId - Document ID
 * @returns {string} URL path for file download
 */
export function getVehicleDocumentDownloadUrl(vehicleId, docId) {
  return `${BASE_PATH}/${vehicleId}/documents/${docId}/download`;
}

/**
 * Re-uploads a pending or rejected vehicle document.
 * @param {number} vehicleId - Vehicle ID
 * @param {number} docId - Document ID
 * @param {File} file - New file to replace the existing one
 * @returns {Promise} Axios response with the updated document
 */
export function reuploadVehicleDocument(vehicleId, docId, file) {
  const formData = new FormData();
  formData.append(FIELD_FILE, file);
  return api.put(`${BASE_PATH}/${vehicleId}/documents/${docId}/reupload`, formData, {
    headers: MULTIPART_HEADERS,
  });
}

