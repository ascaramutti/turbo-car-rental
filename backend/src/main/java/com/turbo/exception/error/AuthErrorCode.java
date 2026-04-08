package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    EMAIL_ALREADY_REGISTERED("AUTH-001", "Email already registered", HttpStatus.CONFLICT),
    USER_NOT_FOUND("AUTH-002", "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_VERIFIED("AUTH-003", "Email is already verified", HttpStatus.CONFLICT),
    INVALID_VERIFICATION_CODE("AUTH-004", "Invalid verification code", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("AUTH-005", "Verification code has expired. Please request a new one.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("AUTH-006", "Please verify your email before logging in.", HttpStatus.FORBIDDEN),
    INVALID_ROLE("AUTH-007", "Invalid role. Must be DRIVER or CAR_OWNER", HttpStatus.BAD_REQUEST),
    UNDERAGE_USER("AUTH-008", "You must be at least 19 years old to register.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
