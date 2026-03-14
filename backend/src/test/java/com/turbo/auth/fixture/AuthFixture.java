package com.turbo.auth.fixture;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.dto.LoginRequest;
import com.turbo.auth.dto.MessageResponse;
import com.turbo.auth.dto.RegisterRequest;
import com.turbo.auth.dto.RegisterResponse;
import com.turbo.auth.dto.ResendOtpRequest;
import com.turbo.auth.dto.VerifyOtpRequest;
import com.turbo.auth.service.command.LoginCommand;
import com.turbo.auth.service.command.RegisterCommand;
import com.turbo.auth.service.mapper.AuthServiceMapper;
import com.turbo.common.TestUtil;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;

import java.time.LocalDateTime;

public final class AuthFixture {

    private static final String FIXTURES_PATH = "fixtures/auth/";

    public static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    public static final String VALID_OTP = "123456";
    public static final String VALID_JWT = "eyJhbGciOiJIUzI1NiJ9.test.token";
    public static final int OTP_EXPIRY_MINUTES = 10;

    private AuthFixture() {}

    // ── Register requests from JSON ────────────────────────────────────

    public static RegisterRequest driverRegisterRequest() {
        return loadFixture("register-driver-request.json", RegisterRequest.class);
    }

    public static RegisterRequest ownerRegisterRequest() {
        return loadFixture("register-owner-request.json", RegisterRequest.class);
    }

    public static RegisterRequest lowercaseRoleRequest() {
        return loadFixture("register-lowercase-role-request.json", RegisterRequest.class);
    }

    public static RegisterRequest mixedCaseRoleRequest() {
        return loadFixture("register-mixedcase-role-request.json", RegisterRequest.class);
    }

    public static RegisterRequest invalidRoleRequest() {
        return loadFixture("register-invalid-role-request.json", RegisterRequest.class);
    }

    public static RegisterRequest adminRoleRequest() {
        return loadFixture("register-admin-role-request.json", RegisterRequest.class);
    }

    public static RegisterRequest emptyRoleRequest() {
        return loadFixture("register-empty-role-request.json", RegisterRequest.class);
    }

    public static RegisterRequest underageRequest() {
        return loadFixture("register-underage-request.json", RegisterRequest.class);
    }

    // ── Register commands from JSON ────────────────────────────────────

    public static RegisterCommand driverRegisterCommand() {
        return loadFixture("register-driver-request.json", RegisterCommand.class);
    }

    public static RegisterCommand ownerRegisterCommand() {
        return loadFixture("register-owner-request.json", RegisterCommand.class);
    }

    public static RegisterCommand lowercaseRoleCommand() {
        return loadFixture("register-lowercase-role-request.json", RegisterCommand.class);
    }

    public static RegisterCommand mixedCaseRoleCommand() {
        return loadFixture("register-mixedcase-role-request.json", RegisterCommand.class);
    }

    public static RegisterCommand invalidRoleCommand() {
        return loadFixture("register-invalid-role-request.json", RegisterCommand.class);
    }

    public static RegisterCommand adminRoleCommand() {
        return loadFixture("register-admin-role-request.json", RegisterCommand.class);
    }

    public static RegisterCommand emptyRoleCommand() {
        return loadFixture("register-empty-role-request.json", RegisterCommand.class);
    }

    public static RegisterCommand underageCommand() {
        return loadFixture("register-underage-request.json", RegisterCommand.class);
    }

    // ── Other requests from JSON ───────────────────────────────────────

    public static VerifyOtpRequest verifyOtpRequest() {
        return loadFixture("verify-otp-request.json", VerifyOtpRequest.class);
    }

    public static ResendOtpRequest resendOtpRequest() {
        return loadFixture("resend-otp-request.json", ResendOtpRequest.class);
    }

    public static LoginRequest loginRequest() {
        return loadFixture("login-request.json", LoginRequest.class);
    }

    // ── Login command from JSON ────────────────────────────────────────

    public static LoginCommand loginCommand() {
        return loadFixture("login-request.json", LoginCommand.class);
    }

    // ── Entities ───────────────────────────────────────────────────────

    public static Driver newDriver() {
        return new Driver();
    }

    public static CarOwner newCarOwner() {
        return new CarOwner();
    }

    public static User verifiedUser() {
        User user = new Driver();
        user.setUserId(1L);
        user.setEmail("driver@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(UserRole.DRIVER);
        user.setEmailVerified(true);
        user.setPassword(ENCODED_PASSWORD);
        return user;
    }

    public static User unverifiedUserWithOtp() {
        User user = new Driver();
        user.setUserId(1L);
        user.setEmail("driver@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(UserRole.DRIVER);
        user.setEmailVerified(false);
        user.setPassword(ENCODED_PASSWORD);
        user.setOtpCode(VALID_OTP);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        return user;
    }

    public static User unverifiedUserWithExpiredOtp() {
        User user = unverifiedUserWithOtp();
        user.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        return user;
    }

    // ── Responses ──────────────────────────────────────────────────────

    public static RegisterResponse registerResponse(String email) {
        return new RegisterResponse(AuthServiceMapper.REGISTRATION_SUCCESS_MESSAGE, email, true);
    }

    public static AuthResponse authResponse(User user) {
        return new AuthResponse(VALID_JWT, user.getUserId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    public static MessageResponse resendOtpResponse() {
        return new MessageResponse(AuthServiceMapper.RESEND_OTP_SUCCESS_MESSAGE);
    }

    // ── Internal ───────────────────────────────────────────────────────

    private static <T> T loadFixture(String fileName, Class<T> type) {
        return TestUtil.loadFixture(FIXTURES_PATH + fileName, type);
    }
}
