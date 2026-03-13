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

    /** OTP verification validation messages. */
    public static final String OTP_REQUIRED = "OTP code is required";
    public static final String OTP_PATTERN = "OTP must be exactly 6 digits";
}
