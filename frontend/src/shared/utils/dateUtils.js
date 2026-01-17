/**
 * Formats an ISO date string for display.
 * @param {string} dateString - ISO 8601 date string
 * @returns {string} Formatted date string
 */
export function formatDateTime(dateString) {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString();
}
