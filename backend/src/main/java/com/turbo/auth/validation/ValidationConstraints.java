package com.turbo.auth.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationConstraints {

    public static final int EMAIL_MAX = 255;
    public static final int NAME_MAX = 50;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 100;

    /** Address field constraints. */
    public static final int STREET_ADDRESS_MAX = 255;
    public static final int CITY_MAX = 100;
    public static final int PROVINCE_MAX = 100;
    public static final int COUNTRY_MAX = 100;
}
