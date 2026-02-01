package com.turbo.auth.dto;

import com.turbo.auth.validation.ValidationConstraints;
import com.turbo.auth.validation.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResendOtpRequest {

    @Email(message = ValidationMessages.EMAIL_INVALID)
    @NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @Size(max = ValidationConstraints.EMAIL_MAX, message = ValidationMessages.EMAIL_MAX_LENGTH)
    private String email;
}
