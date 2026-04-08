package com.turbo.exception;

import com.turbo.exception.error.AuthErrorCode;
import com.turbo.exception.error.EmailErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── handleBusiness ──────────────────────────────────────────────────

    @Nested
    @DisplayName("handleBusiness()")
    class HandleBusiness {

        @Test
        @DisplayName("AuthErrorCode - returns correct code, message, and HTTP status")
        void handleBusiness_authError_returnsCorrectResponse() {
            BusinessException ex = new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("AUTH-001");
            assertThat(response.getBody().getMessage()).isEqualTo("Email already registered");
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getTimestamp()).isNotBlank();
        }

        @Test
        @DisplayName("EmailErrorCode - returns EMAIL-001 with 500 status")
        void handleBusiness_emailError_returnsCorrectResponse() {
            BusinessException ex = new BusinessException(EmailErrorCode.EMAIL_SEND_FAILED);

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("EMAIL-001");
            assertThat(response.getBody().getMessage()).isEqualTo("Failed to send verification email");
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("USER_NOT_FOUND - returns 404 with AUTH-002")
        void handleBusiness_userNotFound_returns404() {
            BusinessException ex = new BusinessException(AuthErrorCode.USER_NOT_FOUND);

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getCode()).isEqualTo("AUTH-002");
        }

        @Test
        @DisplayName("EMAIL_NOT_VERIFIED - returns 403 with AUTH-006")
        void handleBusiness_emailNotVerified_returns403() {
            BusinessException ex = new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getCode()).isEqualTo("AUTH-006");
        }

        @Test
        @DisplayName("INVALID_ROLE - returns 400 with AUTH-007")
        void handleBusiness_invalidRole_returns400() {
            BusinessException ex = new BusinessException(AuthErrorCode.INVALID_ROLE);

            ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo("AUTH-007");
        }
    }

    // ── handleValidation ────────────────────────────────────────────────

    @Nested
    @DisplayName("handleValidation()")
    class HandleValidation {

        @Test
        @DisplayName("Single field error - returns VALIDATION-001 with field details")
        void handleValidation_singleFieldError_returnsValidationResponse() throws NoSuchMethodException {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "must not be blank"));

            MethodParameter methodParameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod"), -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("VALIDATION-001");
            assertThat(response.getBody().getMessage()).isEqualTo("Validation Failed");
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getTimestamp()).isNotBlank();
            assertThat(response.getBody().getDetails()).containsEntry("email", "must not be blank");
        }

        @Test
        @DisplayName("Multiple field errors - returns all field details")
        void handleValidation_multipleFieldErrors_returnsAllDetails() throws NoSuchMethodException {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "must not be blank"));
            bindingResult.addError(new FieldError("request", "password", "size must be between 6 and 100"));

            MethodParameter methodParameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod"), -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ValidationErrorResponse> response = handler.handleValidation(ex);

            assertThat(response.getBody().getDetails()).hasSize(2);
            assertThat(response.getBody().getDetails()).containsEntry("email", "must not be blank");
            assertThat(response.getBody().getDetails()).containsEntry("password", "size must be between 6 and 100");
        }
    }

    // ── handleBadCredentials ────────────────────────────────────────────

    @Nested
    @DisplayName("handleBadCredentials()")
    class HandleBadCredentials {

        @Test
        @DisplayName("BadCredentialsException - returns AUTH-009 with 401 status")
        void handleBadCredentials_returnsUnauthorized() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("AUTH-009");
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
            assertThat(response.getBody().getStatus()).isEqualTo(401);
            assertThat(response.getBody().getTimestamp()).isNotBlank();
        }
    }

    // ── handleGeneral ───────────────────────────────────────────────────

    @Nested
    @DisplayName("handleGeneral()")
    class HandleGeneral {

        @Test
        @DisplayName("RuntimeException - returns SYS-001 with 500 status")
        void handleGeneral_runtimeException_returns500() {
            Exception ex = new RuntimeException("something broke");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("SYS-001");
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getTimestamp()).isNotBlank();
        }

        @Test
        @DisplayName("NullPointerException - returns SYS-001 with 500 status")
        void handleGeneral_nullPointerException_returns500() {
            Exception ex = new NullPointerException();

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getCode()).isEqualTo("SYS-001");
        }

        @Test
        @DisplayName("Generic Exception - returns SYS-001 with generic message")
        void handleGeneral_genericException_returnsGenericMessage() {
            Exception ex = new Exception("database connection lost");

            ResponseEntity<ErrorResponse> response = handler.handleGeneral(ex);

            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        }
    }

    // Helper method used to create MethodParameter for validation tests
    @SuppressWarnings("unused")
    private void dummyMethod() {}
}
