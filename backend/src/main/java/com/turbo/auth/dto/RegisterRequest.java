package com.turbo.auth.dto;

import com.turbo.auth.validation.ValidationConstraints;
import com.turbo.auth.validation.ValidationMessages;
import com.turbo.auth.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = ValidationMessages.FIRST_NAME_REQUIRED)
    @Size(max = ValidationConstraints.NAME_MAX, message = ValidationMessages.FIRST_NAME_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.FIRST_NAME_PATTERN)
    private String firstName;

    @NotBlank(message = ValidationMessages.LAST_NAME_REQUIRED)
    @Size(max = ValidationConstraints.NAME_MAX, message = ValidationMessages.LAST_NAME_MAX_LENGTH)
    @Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.LAST_NAME_PATTERN)
    private String lastName;

    @Email(message = ValidationMessages.EMAIL_INVALID)
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Size(max = ValidationConstraints.EMAIL_MAX, message = ValidationMessages.EMAIL_MAX_LENGTH)
    private String email;

    @NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @Size(min = ValidationConstraints.PASSWORD_MIN, max = ValidationConstraints.PASSWORD_MAX, message = ValidationMessages.PASSWORD_SIZE)
    private String password;

    @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationMessages.PHONE_PATTERN)
    private String phoneNumber;

    @NotNull(message = ValidationMessages.DATE_OF_BIRTH_REQUIRED)
    @Past(message = ValidationMessages.DATE_OF_BIRTH_PAST)
    private LocalDate dateOfBirth;

    @NotBlank(message = ValidationMessages.ROLE_REQUIRED)
    private String role;

    @Valid
    @NotNull(message = ValidationMessages.ADDRESS_REQUIRED)
    private AddressRequest address;
}
