package com.turbo.auth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OtpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_MIN_VALUE = 100_000;
    private static final int OTP_RANGE = 900_000;

    /** Generates a random 6-digit OTP code. */
    public static String generate() {
        int otp = OTP_MIN_VALUE + RANDOM.nextInt(OTP_RANGE);
        return String.valueOf(otp);
    }
}
