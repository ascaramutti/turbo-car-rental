package com.turbo.auth.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationMessages {

    /** Common validation messages shared across requests. */
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Invalid email format";
    public static final String EMAIL_MAX_LENGTH = "Email must not exceed 255 characters";
    public static final String PASSWORD_REQUIRED = "Password is required";

    /** Registration-specific validation messages. */
    public static final String FIRST_NAME_REQUIRED = "First name is required";
    public static final String FIRST_NAME_MAX_LENGTH = "First name must not exceed 50 characters";
    public static final String FIRST_NAME_PATTERN = "First name can only contain letters, accents, apostrophes, and hyphens";
    public static final String LAST_NAME_REQUIRED = "Last name is required";
    public static final String LAST_NAME_MAX_LENGTH = "Last name must not exceed 50 characters";
    public static final String LAST_NAME_PATTERN = "Last name can only contain letters, accents, apostrophes, and hyphens";
    public static final String PASSWORD_SIZE = "Password must be between 6 and 100 characters";
    public static final String PHONE_PATTERN = "Invalid phone number format";
    public static final String DATE_OF_BIRTH_REQUIRED = "Date of birth is required";
    public static final String DATE_OF_BIRTH_PAST = "Date of birth must be in the past";
    public static final String ROLE_REQUIRED = "Role is required (DRIVER or CAR_OWNER)";

    /** Address validation messages. */
    public static final String ADDRESS_REQUIRED = "Address is required";
    public static final String STREET_ADDRESS_REQUIRED = "Street address is required";
    public static final String STREET_ADDRESS_MAX_LENGTH = "Street address must not exceed 255 characters";
    public static final String STREET_ADDRESS_PATTERN = "Street address can only contain letters, numbers, spaces, dots, commas, #, hyphens, and slashes";
    public static final String CITY_REQUIRED = "City is required";
    public static final String CITY_MAX_LENGTH = "City must not exceed 100 characters";
    public static final String CITY_PATTERN = "City can only contain letters, spaces, dots, hyphens, and apostrophes";
    public static final String PROVINCE_REQUIRED = "Province is required";
    public static final String PROVINCE_MAX_LENGTH = "Province must not exceed 100 characters";
    public static final String PROVINCE_PATTERN = "Province can only contain letters, spaces, dots, hyphens, and apostrophes";
    public static final String POSTAL_CODE_REQUIRED = "Postal code is required";
    public static final String POSTAL_CODE_PATTERN = "Invalid postal code format (expected: A1A 1A1)";
    public static final String COUNTRY_REQUIRED = "Country is required";
    public static final String COUNTRY_MAX_LENGTH = "Country must not exceed 100 characters";
    public static final String COUNTRY_PATTERN = "Country can only contain letters, spaces, dots, hyphens, and apostrophes";

    /** OTP verification validation messages. */
    public static final String OTP_REQUIRED = "OTP code is required";
    public static final String OTP_PATTERN = "OTP must be exactly 6 digits";
}
