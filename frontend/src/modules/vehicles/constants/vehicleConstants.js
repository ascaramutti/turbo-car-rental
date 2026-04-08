/**
 * Vehicle module constants matching the backend API contract (Module 3).
 */

/** Supported vehicle category identifiers. */
export const VEHICLE_CATEGORIES = {
  SEDAN: 'SEDAN',
  SUV: 'SUV',
  VAN: 'VAN',
  TRUCK: 'TRUCK',
  COMPACT: 'COMPACT',
};

/** Supported fuel type identifiers. */
export const FUEL_TYPES = {
  GASOLINE: 'GASOLINE',
  DIESEL: 'DIESEL',
  ELECTRIC: 'ELECTRIC',
  HYBRID: 'HYBRID',
};

/** Possible vehicle statuses (admin approval lifecycle). */
export const VEHICLE_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  INACTIVE: 'INACTIVE',
};

/** Human-readable labels for each vehicle category. */
export const CATEGORY_LABELS = {
  [VEHICLE_CATEGORIES.SEDAN]: 'Sedan',
  [VEHICLE_CATEGORIES.SUV]: 'SUV',
  [VEHICLE_CATEGORIES.VAN]: 'Van',
  [VEHICLE_CATEGORIES.TRUCK]: 'Truck',
  [VEHICLE_CATEGORIES.COMPACT]: 'Compact',
};

/** Human-readable labels for each fuel type. */
export const FUEL_TYPE_LABELS = {
  [FUEL_TYPES.GASOLINE]: 'Gasoline',
  [FUEL_TYPES.DIESEL]: 'Diesel',
  [FUEL_TYPES.ELECTRIC]: 'Electric',
  [FUEL_TYPES.HYBRID]: 'Hybrid',
};

/** UI styling configuration for each vehicle status. */
export const STATUS_CONFIG = {
  [VEHICLE_STATUS.PENDING]: {
    color: 'text-amber-500',
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    label: 'Pending Docs',
  },
  [VEHICLE_STATUS.APPROVED]: {
    color: 'text-blue-600',
    bg: 'bg-blue-50',
    border: 'border-blue-200',
    label: 'Approved',
  },
  [VEHICLE_STATUS.INACTIVE]: {
    color: 'text-gray-500',
    bg: 'bg-gray-50',
    border: 'border-gray-200',
    label: 'Inactive',
  },
};

/** Document types required for a vehicle to become active. */
export const VEHICLE_DOCUMENT_TYPES = {
  INSURANCE: 'INSURANCE',
  VEHICLE_REGISTRATION: 'VEHICLE_REGISTRATION',
  INSPECTION_REPORT: 'INSPECTION_REPORT',
};

/** Human-readable labels for each vehicle document type. */
export const VEHICLE_DOCUMENT_LABELS = {
  [VEHICLE_DOCUMENT_TYPES.INSURANCE]: 'Insurance Certificate',
  [VEHICLE_DOCUMENT_TYPES.VEHICLE_REGISTRATION]: 'Vehicle Registration',
  [VEHICLE_DOCUMENT_TYPES.INSPECTION_REPORT]: 'Inspection Report (CVIP)',
};

/** Required vehicle document types that must be uploaded for approval. */
export const REQUIRED_VEHICLE_DOCUMENT_TYPES = [
  VEHICLE_DOCUMENT_TYPES.INSURANCE,
  VEHICLE_DOCUMENT_TYPES.VEHICLE_REGISTRATION,
];

/** Optional vehicle document types. INSPECTION_REPORT is only required for TAXI_AND_DELIVERY. */
export const OPTIONAL_VEHICLE_DOCUMENT_TYPES = [
  VEHICLE_DOCUMENT_TYPES.INSPECTION_REPORT,
];

/** All vehicle document types (required + optional). */
export const ALL_VEHICLE_DOCUMENT_TYPES = [
  ...REQUIRED_VEHICLE_DOCUMENT_TYPES,
  ...OPTIONAL_VEHICLE_DOCUMENT_TYPES,
];

/** Document review statuses (shared with Module 2). */
export const DOCUMENT_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

/** UI styling configuration for each document status. */
export const DOCUMENT_STATUS_CONFIG = {
  [DOCUMENT_STATUS.PENDING]: {
    color: 'text-amber-500',
    bg: 'bg-amber-50',
    border: 'border-amber-200',
    label: 'Under Review',
  },
  [DOCUMENT_STATUS.APPROVED]: {
    color: 'text-green-600',
    bg: 'bg-green-50',
    border: 'border-green-200',
    label: 'Approved',
  },
  [DOCUMENT_STATUS.REJECTED]: {
    color: 'text-red-500',
    bg: 'bg-red-50',
    border: 'border-red-200',
    label: 'Rejected',
  },
};

/** Service type classification assigned by admin on vehicle activation. */
export const SERVICE_TYPES = {
  TAXI_AND_DELIVERY: 'TAXI_AND_DELIVERY',
  DELIVERY_ONLY: 'DELIVERY_ONLY',
};

/** Human-readable labels for each service type. */
export const SERVICE_TYPE_LABELS = {
  [SERVICE_TYPES.TAXI_AND_DELIVERY]: 'Taxi + Delivery',
  [SERVICE_TYPES.DELIVERY_ONLY]: 'Delivery Only',
};

/** UI styling for service type badges. */
export const SERVICE_TYPE_CONFIG = {
  [SERVICE_TYPES.TAXI_AND_DELIVERY]: { color: 'text-blue-600', bg: 'bg-blue-50', border: 'border-blue-200' },
  [SERVICE_TYPES.DELIVERY_ONLY]: { color: 'text-amber-600', bg: 'bg-amber-50', border: 'border-amber-200' },
};

/** VIN (Vehicle Identification Number) length. */
export const VIN_LENGTH = 17;

/** Minimum hourly rate in dollars. */
export const MIN_HOURLY_RATE = 1.0;

/** Number of years a vehicle can be old to be registered on the platform. */
export const MAX_VEHICLE_AGE_YEARS = 20;

/** Maximum vehicle age (in years) eligible for TAXI_AND_DELIVERY service type. */
export const MAX_TAXI_ELIGIBLE_AGE_YEARS = 9;
