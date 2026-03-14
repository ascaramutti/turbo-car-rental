package com.turbo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "myTestSecretKeyThatIsAtLeast32BytesLongForHmacSha256!";
    private static final long EXPIRATION = 86400000L; // 24 hours
    private static final String EMAIL = "driver@test.com";
    private static final Long USER_ID = 1L;
    private static final String ROLE = "DRIVER";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    // ── generateToken ───────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("Valid params - returns non-null JWT string")
        void generateToken_validParams_returnsJwt() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    // ── extractEmail ────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractEmail()")
    class ExtractEmail {

        @Test
        @DisplayName("Valid token - returns the email used to generate it")
        void extractEmail_validToken_returnsEmail() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            String extractedEmail = jwtUtil.extractEmail(token);

            assertThat(extractedEmail).isEqualTo(EMAIL);
        }
    }

    // ── isTokenValid ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("Valid token + matching email - returns true")
        void isTokenValid_matchingEmail_returnsTrue() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            assertThat(jwtUtil.isTokenValid(token, EMAIL)).isTrue();
        }

        @Test
        @DisplayName("Valid token + different email - returns false")
        void isTokenValid_differentEmail_returnsFalse() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            assertThat(jwtUtil.isTokenValid(token, "other@test.com")).isFalse();
        }

        @Test
        @DisplayName("Expired token - throws ExpiredJwtException on validation")
        void isTokenValid_expiredToken_throwsException() {
            JwtUtil shortLivedJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(shortLivedJwtUtil, "secret", SECRET);
            ReflectionTestUtils.setField(shortLivedJwtUtil, "expiration", -1000L); // already expired

            String token = shortLivedJwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            assertThatThrownBy(() -> jwtUtil.isTokenValid(token, EMAIL))
                    .isInstanceOf(Exception.class);
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Malformed token - throws exception on extractEmail")
        void extractEmail_malformedToken_throwsException() {
            assertThatThrownBy(() -> jwtUtil.extractEmail("not.a.valid.jwt"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Tampered token - throws exception on validation")
        void isTokenValid_tamperedToken_throwsException() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThatThrownBy(() -> jwtUtil.isTokenValid(tampered, EMAIL))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Wrong secret - throws exception on extractEmail")
        void extractEmail_wrongSecret_throwsException() {
            String token = jwtUtil.generateToken(EMAIL, USER_ID, ROLE);

            JwtUtil otherJwtUtil = new JwtUtil();
            ReflectionTestUtils.setField(otherJwtUtil, "secret", "aCompletelyDifferentSecretKeyThatIs32BytesLong!!");
            ReflectionTestUtils.setField(otherJwtUtil, "expiration", EXPIRATION);

            assertThatThrownBy(() -> otherJwtUtil.extractEmail(token))
                    .isInstanceOf(Exception.class);
        }
    }
}
