package com.turbo.vehicle.validation;

public final class VehicleValidationConstraints {

    private VehicleValidationConstraints() {
    }

    public static final String VIN_PATTERN = "^[A-HJ-NPR-Z0-9]{17}$";
    public static final String TEXT_PATTERN = "^[a-zA-Z0-9À-ÿ\\s\\-'.]+$";
    public static final String LICENSE_PLATE_PATTERN = "^[A-Z0-9\\-\\s]{2,10}$";
    public static final String CATEGORY_PATTERN = "^(SEDAN|SUV|VAN|TRUCK|COMPACT)$";
    public static final String FUEL_TYPE_PATTERN = "^(GASOLINE|DIESEL|ELECTRIC|HYBRID)$";
    public static final String SERVICE_TYPE_PATTERN = "^(TAXI_AND_DELIVERY|DELIVERY_ONLY)$";
    public static final String DESCRIPTION_PATTERN = "^[a-zA-Z0-9À-ÿ\\s.,!?()'+:\\-/]*$";
    public static final String LOCATION_PATTERN = "^[a-zA-Z0-9À-ÿ\\s.,#\\-'/]+$";
    public static final int MAX_VEHICLE_AGE_YEARS = 20;
    public static final int MAX_VEHICLE_FUTURE_YEARS = 1;
    public static final int MAX_TAXI_ELIGIBLE_AGE_YEARS = 9;
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_LOCATION_LENGTH = 100;
}
