import api from '../api/axios';

/** Axios config for fetching binary file content. */
export const BLOB_RESPONSE_CONFIG = { responseType: 'blob' };

/**
 * Downloads a file via authenticated API call.
 * Fetches the blob, creates a temporary link, triggers download, and cleans up.
 * @param {string} downloadUrl - API endpoint path for the file download
 * @param {string} fileName - Name for the downloaded file
 */
export async function downloadFile(downloadUrl, fileName) {
  const response = await api.get(downloadUrl, BLOB_RESPONSE_CONFIG);
  const url = URL.createObjectURL(response.data);
  const link = globalThis.document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
