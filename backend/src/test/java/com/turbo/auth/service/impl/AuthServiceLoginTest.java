package com.turbo.auth.service.impl;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.fixture.AuthFixture;
import com.turbo.auth.service.command.LoginCommand;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - login()")
class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private AuthServiceMapper authServiceMapper;
    @Mock private UserFactory userFactory;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", AuthFixture.OTP_EXPIRY_MINUTES);
    }

    // ── Happy path ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Valid credentials + verified email - returns AuthResponse with JWT")
        void login_validCredentials_success() {
            LoginCommand command = AuthFixture.loginCommand();
            User user = AuthFixture.verifiedUser();
            AuthResponse expectedResponse = AuthFixture.authResponse(user);

            when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(anyString(), any(), anyString())).thenReturn(AuthFixture.VALID_JWT);
            when(authServiceMapper.toAuthResponse(any(User.class), anyString())).thenReturn(expectedResponse);

            AuthResponse result = authService.login(command);

            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    // ── Verifications ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        private LoginCommand command;
        private User user;

        @BeforeEach
        void setUpHappyPath() {
            command = AuthFixture.loginCommand();
            user = AuthFixture.verifiedUser();

            when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(anyString(), any(), anyString())).thenReturn(AuthFixture.VALID_JWT);
            when(authServiceMapper.toAuthResponse(any(User.class), anyString())).thenReturn(AuthFixture.authResponse(user));
        }

        @Test
        @DisplayName("AuthenticationManager is called with email and password")
        void login_authenticatesWithCredentials() {
            authService.login(command);

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken(command.getEmail(), command.getPassword())
            );
        }

        @Test
        @DisplayName("JWT is generated with correct user data")
        void login_generatesJwtWithUserData() {
            authService.login(command);

            verify(jwtUtil).generateToken(user.getEmail(), user.getUserId(), user.getRole().name());
        }

        @Test
        @DisplayName("No save or email interactions on login")
        void login_noSaveOrEmailInteractions() {
            authService.login(command);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(emailService, userFactory, passwordEncoder);
        }
    }

    // ── Unhappy paths ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Bad credentials - AuthenticationManager throws BadCredentialsException")
        void login_badCredentials_throwsBadCredentials() {
            LoginCommand command = AuthFixture.loginCommand();
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(anyString());
            verifyNoInteractions(jwtUtil, authServiceMapper);
        }

        @Test
        @DisplayName("User not found after authentication - throws AUTH-002")
        void login_userNotFound_throwsUserNotFound() {
            LoginCommand command = AuthFixture.loginCommand();
            when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

            verifyNoInteractions(jwtUtil, authServiceMapper);
        }

        @Test
        @DisplayName("Email not verified - throws AUTH-006")
        void login_emailNotVerified_throwsEmailNotVerified() {
            LoginCommand command = AuthFixture.loginCommand();
            User user = AuthFixture.unverifiedUserWithOtp();
            when(userRepository.findByEmail(command.getEmail())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.EMAIL_NOT_VERIFIED);

            verifyNoInteractions(jwtUtil, authServiceMapper);
        }
    }
}
