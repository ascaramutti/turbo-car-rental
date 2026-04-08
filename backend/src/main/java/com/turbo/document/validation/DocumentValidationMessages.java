package com.turbo.document.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentValidationMessages {

    public static final String ACTION_REQUIRED = "Review action is required (APPROVE or REJECT)";
    public static final String ACTION_PATTERN = "Review action must be APPROVE or REJECT";
    public static final String LICENSE_CLASS_PATTERN = "License class must be CLASS_4 or CLASS_5";
    public static final String REJECTION_REASON_MAX_LENGTH = "Rejection reason must not exceed 500 characters";
    public static final String REJECTION_REASON_PATTERN = "Rejection reason can only contain letters, numbers, spaces, and basic punctuation";
}
