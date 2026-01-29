package com.turbo.exception;

import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.exception.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION-001";
    private static final String VALIDATION_ERROR_MESSAGE = "Validation Failed";
    private static final String BAD_CREDENTIALS_CODE = "AUTH-009";
    private static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";
    private static final String SYSTEM_ERROR_CODE = "SYS-001";
    private static final String SYSTEM_ERROR_MESSAGE = "An unexpected error occurred";

    /** Handles business logic exceptions with their specific error codes. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return buildErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }

    /** Handles request validation errors and returns field-level details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = extractFieldErrors(ex);

        ValidationErrorResponse response = new ValidationErrorResponse(
                VALIDATION_ERROR_CODE,
                VALIDATION_ERROR_MESSAGE,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now().toString(),
                details
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /** Handles invalid credentials during login. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(BAD_CREDENTIALS_CODE, BAD_CREDENTIALS_MESSAGE, HttpStatus.UNAUTHORIZED);
    }

    /** Handles @Validated constraint violations on @RequestParam / @PathVariable. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return buildErrorResponse(VALIDATION_ERROR_CODE, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /** Handles file upload size exceeded (Spring multipart limit). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        DocumentErrorCode errorCode = DocumentErrorCode.FILE_TOO_LARGE;
        return buildErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }

    /** Catches any unhandled exception as a generic server error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(SYSTEM_ERROR_CODE, SYSTEM_ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** Extracts field names and error messages from validation errors. */
    private Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        return details;
    }

    /** Builds a standardized error response with code, message, and status. */
    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String message, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(
                code,
                message,
                status.value(),
                LocalDateTime.now().toString()
        );
        return new ResponseEntity<>(response, status);
    }
}
