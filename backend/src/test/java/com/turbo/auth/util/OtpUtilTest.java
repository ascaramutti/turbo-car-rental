package com.turbo.auth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OtpUtil")
class OtpUtilTest {

    // ── Happy paths ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("Returns a 6-digit numeric string")
        void generate_returns6DigitString() {
            String otp = OtpUtil.generate();

            assertThat(otp).matches("\\d{6}");
        }

        @Test
        @DisplayName("OTP is between 100000 and 999999")
        void generate_withinValidRange() {
            String otp = OtpUtil.generate();
            int value = Integer.parseInt(otp);

            assertThat(value).isBetween(100_000, 999_999);
        }

        @RepeatedTest(20)
        @DisplayName("Repeated calls always produce valid 6-digit OTPs")
        void generate_repeatedCalls_alwaysValid() {
            String otp = OtpUtil.generate();

            assertThat(otp).matches("\\d{6}");
            assertThat(Integer.parseInt(otp)).isBetween(100_000, 999_999);
        }

        @Test
        @DisplayName("Multiple calls produce varying OTPs (randomness check)")
        void generate_multipleCalls_producesVariation() {
            Set<String> otps = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                otps.add(OtpUtil.generate());
            }

            assertThat(otps.size()).isGreaterThan(1);
        }
    }
}
