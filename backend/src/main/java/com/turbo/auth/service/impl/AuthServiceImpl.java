package com.turbo.auth.service.impl;

import com.turbo.auth.dto.AuthResponse;
import com.turbo.auth.dto.MessageResponse;
import com.turbo.auth.dto.RegisterResponse;
import com.turbo.auth.service.AuthService;
import com.turbo.auth.service.command.LoginCommand;
import com.turbo.auth.service.command.RegisterCommand;
import com.turbo.auth.service.mapper.AuthServiceMapper;
import com.turbo.auth.util.OtpUtil;
import com.turbo.config.JwtUtil;
import com.turbo.email.service.EmailService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.factory.UserFactory;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;
import com.turbo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuthServiceMapper authServiceMapper;
    private final UserFactory userFactory;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.registration.minimum-age}")
    private int minimumAge;

    @Override
    @Transactional
    public RegisterResponse register(RegisterCommand command) {
        validateEmailAvailable(command.getEmail());
        validateMinimumAge(command.getDateOfBirth());

        UserRole role = parseRole(command.getRole());
        User user = userFactory.createByRole(role);

        String encodedPassword = passwordEncoder.encode(command.getPassword());
        authServiceMapper.updateUserFromCommand(command, encodedPassword, role, user);
        assignOtp(user);

        user = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), user.getOtpCode());

        return authServiceMapper.toRegisterResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(String email, String otp) {
        User user = findUserByEmail(email);
        validateEmailNotVerified(user);
        validateOtpCode(user, otp);

        markEmailAsVerified(user);
        userRepository.save(user);

        return authServiceMapper.toAuthResponse(user, generateJwt(user));
    }

    @Override
    @Transactional
    public MessageResponse resendOtp(String email) {
        User user = findUserByEmail(email);
        validateEmailNotVerified(user);

        assignOtp(user);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), user.getOtpCode());

        return authServiceMapper.toResendOtpResponse();
    }

    @Override
    public AuthResponse login(LoginCommand command) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(command.getEmail(), command.getPassword())
        );

        User user = findUserByEmail(command.getEmail());
        validateEmailIsVerified(user);

        return authServiceMapper.toAuthResponse(user, generateJwt(user));
    }

    /** Throws if the user is under the minimum required age. */
    private void validateMinimumAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < minimumAge) {
            throw new BusinessException(AuthErrorCode.UNDERAGE_USER);
        }
    }

    /** Throws if the email is already registered. */
    private void validateEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    /** Throws if the user's email is already verified. */
    private void validateEmailNotVerified(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_VERIFIED);
        }
    }

    /** Throws if the user's email has not been verified yet. */
    private void validateEmailIsVerified(User user) {
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    /** Throws if the OTP code is invalid or expired. */
    private void validateOtpCode(User user, String otp) {
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }
    }

    /** Parses the role string to UserRole enum (case-insensitive). */
    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_ROLE);
        }
    }

    /** Finds a user by email or throws if not found. */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    /** Generates a new OTP code and sets its expiration. */
    private void assignOtp(User user) {
        user.setOtpCode(OtpUtil.generate());
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
    }

    /** Marks the email as verified and clears the OTP data. */
    private void markEmailAsVerified(User user) {
        user.setEmailVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
    }

    /** Generates a JWT token for the given user. */
    private String generateJwt(User user) {
        return jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole().name());
    }
}
