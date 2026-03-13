package com.turbo.auth.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationConstraints {

    public static final int EMAIL_MAX = 255;
    public static final int NAME_MAX = 50;
    public static final int PASSWORD_MIN = 6;
    public static final int PASSWORD_MAX = 100;
}
