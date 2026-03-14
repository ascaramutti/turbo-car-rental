package com.turbo.auth.service.impl;

import com.turbo.auth.dto.RegisterResponse;
import com.turbo.auth.fixture.AuthFixture;
import com.turbo.auth.service.command.RegisterCommand;
import com.turbo.auth.service.mapper.AuthServiceMapper;
import com.turbo.config.JwtUtil;
import com.turbo.email.service.EmailService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.factory.UserFactory;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - register()")
class AuthServiceRegisterTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService emailService;
    @Mock private AuthServiceMapper authServiceMapper;
    @Mock private UserFactory userFactory;

    @InjectMocks private AuthServiceImpl authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MINIMUM_AGE = 19;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", OTP_EXPIRY_MINUTES);
        ReflectionTestUtils.setField(authService, "minimumAge", MINIMUM_AGE);
    }

    // ── Mock helpers ───────────────────────────────────────────────────

    private void mockEmailAvailable(String email) {
        when(userRepository.existsByEmail(email)).thenReturn(false);
    }

    private void mockPasswordEncoder() {
        when(passwordEncoder.encode(anyString())).thenReturn(AuthFixture.ENCODED_PASSWORD);
    }

    private void mockMapperSetsUserFields(RegisterCommand command, UserRole role) {
        doAnswer(invocation -> {
            User user = invocation.getArgument(3);
            user.setEmail(command.getEmail());
            user.setFirstName(command.getFirstName());
            user.setLastName(command.getLastName());
            user.setPhoneNumber(command.getPhoneNumber());
            user.setDateOfBirth(command.getDateOfBirth());
            user.setPassword(invocation.getArgument(1));
            user.setRole(role);
            user.setIsVerified(false);
            user.setEmailVerified(false);
            return null;
        }).when(authServiceMapper).updateUserFromCommand(eq(command), eq(AuthFixture.ENCODED_PASSWORD), eq(role), any(User.class));
    }

    private void mockSaveReturnsUser() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RegisterResponse mockRegisterResponse(String email) {
        RegisterResponse response = AuthFixture.registerResponse(email);
        when(authServiceMapper.toRegisterResponse(any(User.class))).thenReturn(response);
        return response;
    }

    // ── Happy paths ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Register as DRIVER - returns RegisterResponse and calls all dependencies")
        void register_asDriver_success() {
            RegisterCommand command = AuthFixture.driverRegisterCommand();
            Driver driver = AuthFixture.newDriver();

            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.DRIVER)).thenReturn(driver);
            mockPasswordEncoder();
            mockMapperSetsUserFields(command, UserRole.DRIVER);
            mockSaveReturnsUser();
            RegisterResponse expectedResponse = mockRegisterResponse(command.getEmail());

            RegisterResponse result = authService.register(command);

            assertThat(result).isEqualTo(expectedResponse);

            verify(userRepository).existsByEmail(command.getEmail());
            verify(userFactory).createByRole(UserRole.DRIVER);
            verify(passwordEncoder).encode(command.getPassword());
            verify(authServiceMapper).updateUserFromCommand(eq(command), eq(AuthFixture.ENCODED_PASSWORD), eq(UserRole.DRIVER), any(User.class));
            verify(userRepository).save(any(User.class));
            verify(emailService).sendVerificationEmail(eq(command.getEmail()), eq(command.getFirstName()), anyString());
            verify(authServiceMapper).toRegisterResponse(any(User.class));
        }

        @Test
        @DisplayName("Register as CAR_OWNER - creates CarOwner via factory")
        void register_asCarOwner_success() {
            RegisterCommand command = AuthFixture.ownerRegisterCommand();
            CarOwner carOwner = AuthFixture.newCarOwner();

            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.CAR_OWNER)).thenReturn(carOwner);
            mockPasswordEncoder();
            mockMapperSetsUserFields(command, UserRole.CAR_OWNER);
            mockSaveReturnsUser();
            mockRegisterResponse(command.getEmail());

            RegisterResponse result = authService.register(command);

            assertThat(result).isNotNull();
            verify(userFactory).createByRole(UserRole.CAR_OWNER);
        }

        @Test
        @DisplayName("Role is case-insensitive - 'driver' maps to DRIVER")
        void register_lowercaseRole_success() {
            RegisterCommand command = AuthFixture.lowercaseRoleCommand();
            Driver driver = AuthFixture.newDriver();

            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.DRIVER)).thenReturn(driver);
            mockPasswordEncoder();
            mockMapperSetsUserFields(command, UserRole.DRIVER);
            mockSaveReturnsUser();
            mockRegisterResponse(command.getEmail());

            RegisterResponse result = authService.register(command);

            assertThat(result).isNotNull();
            verify(userFactory).createByRole(UserRole.DRIVER);
        }

        @Test
        @DisplayName("Mixed case role 'Car_Owner' maps to CAR_OWNER")
        void register_mixedCaseRole_success() {
            RegisterCommand command = AuthFixture.mixedCaseRoleCommand();
            CarOwner carOwner = AuthFixture.newCarOwner();

            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.CAR_OWNER)).thenReturn(carOwner);
            mockPasswordEncoder();
            mockMapperSetsUserFields(command, UserRole.CAR_OWNER);
            mockSaveReturnsUser();
            mockRegisterResponse(command.getEmail());

            RegisterResponse result = authService.register(command);

            assertThat(result).isNotNull();
            verify(userFactory).createByRole(UserRole.CAR_OWNER);
        }
    }

    // ── Verifications on happy path ────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        private RegisterCommand command;

        @BeforeEach
        void setUpHappyPath() {
            command = AuthFixture.driverRegisterCommand();
            Driver driver = AuthFixture.newDriver();

            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.DRIVER)).thenReturn(driver);
            mockPasswordEncoder();
            mockMapperSetsUserFields(command, UserRole.DRIVER);
            mockSaveReturnsUser();
            mockRegisterResponse(command.getEmail());
        }

        @Test
        @DisplayName("Password is encoded before passing to mapper")
        void register_encodesPasswordBeforeMapping() {
            authService.register(command);

            verify(passwordEncoder).encode(command.getPassword());
            verify(authServiceMapper).updateUserFromCommand(eq(command), eq(AuthFixture.ENCODED_PASSWORD), eq(UserRole.DRIVER), any(User.class));
        }

        @Test
        @DisplayName("OTP is assigned with 6-digit code and future expiration")
        void register_assignsOtpWithValidFormatAndExpiry() {
            authService.register(command);

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getOtpCode())
                    .matches("\\d{6}");
            assertThat(savedUser.getOtpExpiresAt())
                    .isAfter(LocalDateTime.now())
                    .isBefore(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES + 1));
        }

        @Test
        @DisplayName("Verification email is sent with correct email, name, and OTP")
        void register_sendsVerificationEmail() {
            authService.register(command);

            verify(userRepository).save(userCaptor.capture());
            String assignedOtp = userCaptor.getValue().getOtpCode();

            verify(emailService).sendVerificationEmail(command.getEmail(), command.getFirstName(), assignedOtp);
        }

        @Test
        @DisplayName("User is persisted before sending email")
        void register_savesUserBeforeSendingEmail() {
            authService.register(command);

            var inOrder = inOrder(userRepository, emailService);
            inOrder.verify(userRepository).save(any(User.class));
            inOrder.verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("No additional repository calls beyond existsByEmail and save")
        void register_noExtraRepositoryCalls() {
            authService.register(command);

            verify(userRepository).existsByEmail(command.getEmail());
            verify(userRepository).save(any(User.class));
            verifyNoMoreInteractions(userRepository);
        }
    }

    // ── Unhappy paths ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Email already registered - throws AUTH-001")
        void register_emailAlreadyExists_throwsEmailAlreadyRegistered() {
            RegisterCommand command = AuthFixture.driverRegisterCommand();
            when(userRepository.existsByEmail(command.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.EMAIL_ALREADY_REGISTERED);

            verifyNoInteractions(userFactory, passwordEncoder, authServiceMapper, emailService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Invalid role string 'BLAH' - throws AUTH-007")
        void register_invalidRoleString_throwsInvalidRole() {
            RegisterCommand command = AuthFixture.invalidRoleCommand();
            mockEmailAvailable(command.getEmail());

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_ROLE);

            verifyNoInteractions(userFactory, passwordEncoder, authServiceMapper, emailService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("ADMIN role - throws AUTH-007 from UserFactory")
        void register_adminRole_throwsInvalidRole() {
            RegisterCommand command = AuthFixture.adminRoleCommand();
            mockEmailAvailable(command.getEmail());
            when(userFactory.createByRole(UserRole.ADMIN))
                    .thenThrow(new BusinessException(AuthErrorCode.INVALID_ROLE));

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_ROLE);

            verifyNoInteractions(passwordEncoder, authServiceMapper, emailService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Empty role string - throws AUTH-007")
        void register_emptyRole_throwsInvalidRole() {
            RegisterCommand command = AuthFixture.emptyRoleCommand();
            mockEmailAvailable(command.getEmail());

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_ROLE);
        }

        @Test
        @DisplayName("Underage user (under 19) - throws AUTH-008")
        void register_underageUser_throwsUnderageUser() {
            RegisterCommand command = AuthFixture.underageCommand();
            mockEmailAvailable(command.getEmail());

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.UNDERAGE_USER);

            verifyNoInteractions(userFactory, passwordEncoder, authServiceMapper, emailService);
            verify(userRepository, never()).save(any());
        }
    }
}
