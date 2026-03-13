package com.turbo.auth.service.impl;

import com.turbo.auth.dto.MessageResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - resendOtp()")
class AuthServiceResendOtpTest {

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
        @DisplayName("Resend OTP - generates new code, saves, sends email, returns message")
        void resendOtp_success() {
            User user = AuthFixture.unverifiedUserWithOtp();
            String oldOtp = user.getOtpCode();
            MessageResponse expectedResponse = AuthFixture.resendOtpResponse();

            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceMapper.toResendOtpResponse()).thenReturn(expectedResponse);

            MessageResponse result = authService.resendOtp(user.getEmail());

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
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceMapper.toResendOtpResponse()).thenReturn(AuthFixture.resendOtpResponse());
        }

        @Test
        @DisplayName("New OTP is assigned with 6-digit code and future expiry")
        void resendOtp_assignsNewOtp() {
            authService.resendOtp(user.getEmail());

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getOtpCode())
                    .matches("\\d{6}");
            assertThat(savedUser.getOtpExpiresAt())
                    .isAfter(LocalDateTime.now())
                    .isBefore(LocalDateTime.now().plusMinutes(AuthFixture.OTP_EXPIRY_MINUTES + 1));
        }

        @Test
        @DisplayName("Email is sent with the new OTP")
        void resendOtp_sendsEmailWithNewOtp() {
            authService.resendOtp(user.getEmail());

            verify(userRepository).save(userCaptor.capture());
            String newOtp = userCaptor.getValue().getOtpCode();

            verify(emailService).sendVerificationEmail(user.getEmail(), user.getFirstName(), newOtp);
        }

        @Test
        @DisplayName("Save happens before sending email")
        void resendOtp_savesBeforeSendingEmail() {
            authService.resendOtp(user.getEmail());

            var inOrder = inOrder(userRepository, emailService);
            inOrder.verify(userRepository).save(any(User.class));
            inOrder.verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("No interactions with JWT or password encoder")
        void resendOtp_noJwtOrPasswordInteractions() {
            authService.resendOtp(user.getEmail());

            verifyNoInteractions(jwtUtil, passwordEncoder, authenticationManager);
        }
    }

    // ── Unhappy paths ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("User not found - throws AUTH-002")
        void resendOtp_userNotFound_throwsUserNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp("unknown@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(emailService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Email already verified - throws AUTH-003")
        void resendOtp_alreadyVerified_throwsEmailAlreadyVerified() {
            User user = AuthFixture.verifiedUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resendOtp(user.getEmail()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.EMAIL_ALREADY_VERIFIED);

            verifyNoInteractions(emailService);
            verify(userRepository, never()).save(any());
        }
    }
}
