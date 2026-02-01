package com.turbo.auth.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationPatterns {

    public static final String NAME = "^[a-zA-ZÀ-ÿ' -]+$";
    public static final String PHONE = "^\\+?[0-9\\s()\\-]{7,20}$";
    public static final String OTP = "^\\d{6}$";

    /** Address patterns — whitelist allowed characters to prevent injection. */
    public static final String STREET_ADDRESS = "^[a-zA-Z0-9À-ÿ\\s.,#\\-/]+$";
    public static final String CITY = "^[a-zA-ZÀ-ÿ\\s.\\-']+$";
    public static final String POSTAL_CODE = "^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$";
}
