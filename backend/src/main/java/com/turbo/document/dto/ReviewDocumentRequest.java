package com.turbo.document.dto;

import com.turbo.document.validation.DocumentValidationConstraints;
import com.turbo.document.validation.DocumentValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewDocumentRequest {

    @NotBlank(message = DocumentValidationMessages.ACTION_REQUIRED)
    @Pattern(regexp = DocumentValidationConstraints.ACTION_PATTERN, message = DocumentValidationMessages.ACTION_PATTERN)
    private String action;

    @Pattern(regexp = DocumentValidationConstraints.LICENSE_CLASS_PATTERN, message = DocumentValidationMessages.LICENSE_CLASS_PATTERN)
    private String licenseClass;

    @Size(max = DocumentValidationConstraints.MAX_REJECTION_REASON_LENGTH, message = DocumentValidationMessages.REJECTION_REASON_MAX_LENGTH)
    @Pattern(regexp = DocumentValidationConstraints.REJECTION_REASON_PATTERN, message = DocumentValidationMessages.REJECTION_REASON_PATTERN)
    private String rejectionReason;
}
