package com.turbo.auth.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationPatterns {

    public static final String NAME = "^[a-zA-ZÀ-ÿ' -]+$";
    public static final String PHONE = "^\\+?[0-9\\s()\\-]{7,20}$";
    public static final String OTP = "^\\d{6}$";
}
