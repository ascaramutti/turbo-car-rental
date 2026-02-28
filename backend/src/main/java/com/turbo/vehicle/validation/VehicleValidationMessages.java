package com.turbo.vehicle.validation;

public final class VehicleValidationMessages {

    private VehicleValidationMessages() {
    }

    public static final String VIN_REQUIRED = "VIN is required";
    public static final String VIN_PATTERN = "VIN must be exactly 17 alphanumeric characters (excluding I, O, Q)";
    public static final String MAKE_REQUIRED = "Vehicle make is required";
    public static final String MAKE_PATTERN = "Make can only contain letters, numbers, spaces, hyphens, and apostrophes";
    public static final String MODEL_REQUIRED = "Vehicle model is required";
    public static final String MODEL_PATTERN = "Model can only contain letters, numbers, spaces, hyphens, and apostrophes";
    public static final String YEAR_REQUIRED = "Vehicle year is required";
    public static final String LICENSE_PLATE_REQUIRED = "License plate is required";
    public static final String LICENSE_PLATE_PATTERN_MSG = "License plate must be 2-10 uppercase alphanumeric characters";
    public static final String CATEGORY_REQUIRED = "Vehicle category is required";
    public static final String CATEGORY_PATTERN_MSG = "Category must be one of: SEDAN, SUV, VAN, TRUCK, COMPACT";
    public static final String FUEL_TYPE_REQUIRED = "Fuel type is required";
    public static final String FUEL_TYPE_PATTERN_MSG = "Fuel type must be one of: GASOLINE, DIESEL, ELECTRIC, HYBRID";
    public static final String HOURLY_RATE_REQUIRED = "Hourly rate is required";
    public static final String HOURLY_RATE_MIN = "Hourly rate must be at least $1.00";
    public static final String LOCATION_REQUIRED = "General location is required";
    public static final String LATITUDE_REQUIRED = "Latitude is required";
    public static final String LONGITUDE_REQUIRED = "Longitude is required";
    public static final String SERVICE_TYPE_REQUIRED = "Service type is required";
    public static final String SERVICE_TYPE_PATTERN_MSG = "Service type must be one of: TAXI_AND_DELIVERY, DELIVERY_ONLY";
    public static final String DESCRIPTION_PATTERN_MSG = "Description can only contain letters, numbers, spaces, and basic punctuation";
    public static final String LOCATION_PATTERN_MSG = "Location can only contain letters, numbers, spaces, commas, periods, hyphens, and #";
    public static final String AVAILABLE_UNTIL_REQUIRED = "Available until date/time is required";
    public static final String AVAILABLE_UNTIL_FUTURE = "Available until must be in the future";
}
