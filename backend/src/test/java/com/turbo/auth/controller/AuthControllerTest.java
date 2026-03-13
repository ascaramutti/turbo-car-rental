package com.turbo.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.turbo.auth.controller.mapper.AuthControllerMapper;
import com.turbo.auth.dto.*;
import com.turbo.auth.fixture.AuthFixture;
import com.turbo.auth.service.AuthService;
import com.turbo.auth.service.command.LoginCommand;
import com.turbo.auth.service.command.RegisterCommand;
import com.turbo.auth.service.mapper.AuthServiceMapper;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private AuthControllerMapper controllerMapper;

    @InjectMocks private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /api/auth/register ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Valid request - returns 200 with RegisterResponse")
        void register_validRequest_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            RegisterCommand command = AuthFixture.driverRegisterCommand();
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(command);
            when(authService.register(command)).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(request.getEmail()))
                    .andExpect(jsonPath("$.message").value(AuthServiceMapper.REGISTRATION_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.requiresVerification").value(true));
        }

        @Test
        @DisplayName("Name with accents (Maria Garcia) - returns 200")
        void register_nameWithAccents_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setFirstName("María");
            request.setLastName("García");
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Name with apostrophe and hyphen (O'Brien Jean-Pierre) - returns 200")
        void register_nameWithApostropheAndHyphen_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setFirstName("Jean-Pierre");
            request.setLastName("O'Brien");
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Phone number with country code (+1 604 555 0001) - returns 200")
        void register_phoneWithCountryCode_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPhoneNumber("+1 604 555 0001");
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Phone number with parentheses ((604) 555-0001) - returns 200")
        void register_phoneWithParentheses_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPhoneNumber("(604) 555-0001");
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Null phone number (optional field) - returns 200")
        void register_nullPhoneNumber_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPhoneNumber(null);
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Password at exact minimum (6 chars) - returns 200")
        void register_passwordExactMinimum_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPassword("abc123");
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Password at exact maximum (100 chars) - returns 200")
        void register_passwordExactMaximum_returns200() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPassword("A".repeat(100));
            RegisterResponse response = AuthFixture.registerResponse(request.getEmail());

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(AuthFixture.driverRegisterCommand());
            when(authService.register(any(RegisterCommand.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Missing email - returns 400 validation error")
        void register_missingEmail_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setEmail("");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Missing password - returns 400 validation error")
        void register_missingPassword_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPassword("");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Short password - returns 400 validation error")
        void register_shortPassword_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPassword("123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("First name with numbers - returns 400 validation error")
        void register_firstNameWithNumbers_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setFirstName("John123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("First name with script tag - returns 400 validation error")
        void register_firstNameWithScript_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setFirstName("<script>alert('xss')</script>");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Last name with special characters - returns 400 validation error")
        void register_lastNameWithSpecialChars_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setLastName("Doe@#$");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("First name too long - returns 400 validation error")
        void register_firstNameTooLong_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setFirstName("A".repeat(51));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Password too long - returns 400 validation error")
        void register_passwordTooLong_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPassword("A".repeat(101));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Invalid phone number format - returns 400 validation error")
        void register_invalidPhoneNumber_returns400() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            request.setPhoneNumber("not-a-phone");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Email already registered - returns 409 with AUTH-001")
        void register_emailExists_returns409() throws Exception {
            RegisterRequest request = AuthFixture.driverRegisterRequest();
            RegisterCommand command = AuthFixture.driverRegisterCommand();

            when(controllerMapper.toRegisterCommand(any(RegisterRequest.class))).thenReturn(command);
            when(authService.register(command))
                    .thenThrow(new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AUTH-001"));
        }
    }

    // ── POST /api/auth/verify-otp ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/verify-otp")
    class VerifyOtp {

        @Test
        @DisplayName("Valid OTP - returns 200 with AuthResponse")
        void verifyOtp_validRequest_returns200() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            User user = AuthFixture.verifiedUser();
            AuthResponse response = AuthFixture.authResponse(user);
            when(authService.verifyOtp(anyString(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(AuthFixture.VALID_JWT))
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.role").value("DRIVER"));
        }

        @Test
        @DisplayName("OTP with leading zeros (000001) - returns 200")
        void verifyOtp_otpWithLeadingZeros_returns200() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("000001");
            User user = AuthFixture.verifiedUser();
            AuthResponse response = AuthFixture.authResponse(user);
            when(authService.verifyOtp(anyString(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OTP at max value (999999) - returns 200")
        void verifyOtp_otpMaxValue_returns200() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("999999");
            User user = AuthFixture.verifiedUser();
            AuthResponse response = AuthFixture.authResponse(user);
            when(authService.verifyOtp(anyString(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Missing OTP code - returns 400 validation error")
        void verifyOtp_missingOtp_returns400() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("");

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("OTP with letters - returns 400 validation error")
        void verifyOtp_otpWithLetters_returns400() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("abcdef");

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("OTP too short (5 digits) - returns 400 validation error")
        void verifyOtp_otpTooShort_returns400() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("12345");

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("OTP too long (7 digits) - returns 400 validation error")
        void verifyOtp_otpTooLong_returns400() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            request.setOtp("1234567");

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Invalid OTP - returns 400 with AUTH-004")
        void verifyOtp_invalidCode_returns400() throws Exception {
            VerifyOtpRequest request = AuthFixture.verifyOtpRequest();
            when(authService.verifyOtp(anyString(), anyString()))
                    .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_CODE));

            mockMvc.perform(post("/api/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUTH-004"));
        }
    }

    // ── POST /api/auth/resend-otp ──────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/resend-otp")
    class ResendOtp {

        @Test
        @DisplayName("Valid request - returns 200 with success message")
        void resendOtp_validRequest_returns200() throws Exception {
            ResendOtpRequest request = AuthFixture.resendOtpRequest();
            MessageResponse response = AuthFixture.resendOtpResponse();
            when(authService.resendOtp(anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/resend-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(AuthServiceMapper.RESEND_OTP_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("Missing email - returns 400 validation error")
        void resendOtp_missingEmail_returns400() throws Exception {
            ResendOtpRequest request = new ResendOtpRequest();
            request.setEmail("");

            mockMvc.perform(post("/api/auth/resend-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }
    }

    // ── POST /api/auth/login ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Valid credentials - returns 200 with AuthResponse")
        void login_validRequest_returns200() throws Exception {
            LoginRequest request = AuthFixture.loginRequest();
            LoginCommand command = AuthFixture.loginCommand();
            User user = AuthFixture.verifiedUser();
            AuthResponse response = AuthFixture.authResponse(user);

            when(controllerMapper.toLoginCommand(any(LoginRequest.class))).thenReturn(command);
            when(authService.login(command)).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(AuthFixture.VALID_JWT))
                    .andExpect(jsonPath("$.email").value(user.getEmail()));
        }

        @Test
        @DisplayName("Missing email - returns 400 validation error")
        void login_missingEmail_returns400() throws Exception {
            LoginRequest request = AuthFixture.loginRequest();
            request.setEmail("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION-001"));
        }

        @Test
        @DisplayName("Email not verified - returns 403 with AUTH-006")
        void login_emailNotVerified_returns403() throws Exception {
            LoginRequest request = AuthFixture.loginRequest();
            LoginCommand command = AuthFixture.loginCommand();

            when(controllerMapper.toLoginCommand(any(LoginRequest.class))).thenReturn(command);
            when(authService.login(command))
                    .thenThrow(new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH-006"));
        }
    }
}
