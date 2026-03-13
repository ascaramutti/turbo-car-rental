package com.turbo.auth.service.impl;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.fixture.AuthFixture;
import com.turbo.auth.service.mapper.AuthServiceMapper;
import com.turbo.config.JwtUtil;
import com.turbo.email.service.EmailService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.factory.UserFactory;
import com.turbo.user.model.User;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - verifyOtp()")
class AuthServiceVerifyOtpTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private AuthServiceMapper authServiceMapper;
    @Mock private UserFactory userFactory;

    @InjectMocks private AuthServiceImpl authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", AuthFixture.OTP_EXPIRY_MINUTES);
    }

    // ── Happy path ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Valid OTP - verifies email, clears token, returns AuthResponse with JWT")
        void verifyOtp_validCode_success() {
            User user = AuthFixture.unverifiedUserWithOtp();
            AuthResponse expectedResponse = AuthFixture.authResponse(user);

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(anyString(), any(), anyString())).thenReturn(AuthFixture.VALID_JWT);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceMapper.toAuthResponse(any(User.class), anyString())).thenReturn(expectedResponse);

            AuthResponse result = authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP);

            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    // ── Verifications ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        private User user;

        @BeforeEach
        void setUpHappyPath() {
            user = AuthFixture.unverifiedUserWithOtp();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(anyString(), any(), anyString())).thenReturn(AuthFixture.VALID_JWT);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceMapper.toAuthResponse(any(User.class), anyString())).thenReturn(AuthFixture.authResponse(user));
        }

        @Test
        @DisplayName("Email is marked as verified and token is cleared")
        void verifyOtp_marksEmailVerifiedAndClearsToken() {
            authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmailVerified()).isTrue();
            assertThat(savedUser.getOtpCode()).isNull();
            assertThat(savedUser.getOtpExpiresAt()).isNull();
        }

        @Test
        @DisplayName("JWT is generated with correct user data")
        void verifyOtp_generatesJwtWithUserData() {
            authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP);

            verify(jwtUtil).generateToken(user.getEmail(), user.getUserId(), user.getRole().name());
        }

        @Test
        @DisplayName("User is saved before generating JWT response")
        void verifyOtp_savesBeforeResponse() {
            authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP);

            var inOrder = inOrder(userRepository, authServiceMapper);
            inOrder.verify(userRepository).save(any(User.class));
            inOrder.verify(authServiceMapper).toAuthResponse(any(User.class), anyString());
        }
    }

    // ── Unhappy paths ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("User not found - throws AUTH-002")
        void verifyOtp_userNotFound_throwsUserNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp("unknown@test.com", AuthFixture.VALID_OTP))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Email already verified - throws AUTH-003")
        void verifyOtp_alreadyVerified_throwsEmailAlreadyVerified() {
            User user = AuthFixture.verifiedUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.EMAIL_ALREADY_VERIFIED);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Invalid OTP code - throws AUTH-004")
        void verifyOtp_wrongCode_throwsInvalidVerificationCode() {
            User user = AuthFixture.unverifiedUserWithOtp();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(user.getEmail(), "999999"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Null verification token on user - throws AUTH-004")
        void verifyOtp_nullToken_throwsInvalidVerificationCode() {
            User user = AuthFixture.unverifiedUserWithOtp();
            user.setOtpCode(null);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        @Test
        @DisplayName("Expired OTP - throws AUTH-005")
        void verifyOtp_expiredCode_throwsVerificationCodeExpired() {
            User user = AuthFixture.unverifiedUserWithExpiredOtp();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(user.getEmail(), AuthFixture.VALID_OTP))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.VERIFICATION_CODE_EXPIRED);

            verify(userRepository, never()).save(any());
        }
    }
}
