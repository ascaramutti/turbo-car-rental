import api from '../../../shared/api/axios';

/** Base API path for admin vehicle endpoints. */
const BASE_PATH = '/admin/vehicles';

/**
 * Checks if a vehicle is ready for classification (all uploaded docs approved).
 * @param {number} vehicleId
 * @returns {Promise} Axios response with classification check data
 */
export function checkVehicleClassification(vehicleId) {
  return api.get(`${BASE_PATH}/${vehicleId}/classification-check`);
}

/**
 * Approves a vehicle with the selected service type.
 * @param {number} vehicleId
 * @param {string} serviceType - TAXI_AND_DELIVERY or DELIVERY_ONLY
 * @returns {Promise} Axios response with the approved vehicle
 */
export function approveVehicle(vehicleId, serviceType) {
  return api.put(`${BASE_PATH}/${vehicleId}/approve`, { serviceType });
}
