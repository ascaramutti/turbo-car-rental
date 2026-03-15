package com.turbo.document.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DocumentValidationConstraints {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_REJECTION_REASON_LENGTH = 500;

    /** Pattern for review action field — only APPROVE or REJECT. */
    public static final String ACTION_PATTERN = "^(APPROVE|REJECT)$";

    /** Pattern for license class field — only CLASS_4 or CLASS_5. */
    public static final String LICENSE_CLASS_PATTERN = "^(CLASS_4|CLASS_5)$";

    /** Pattern for rejection reason — letters, numbers, spaces, basic punctuation. Excludes semicolons and dashes to prevent injection. */
    public static final String REJECTION_REASON_PATTERN = "^[a-zA-Z0-9À-ÿ\\s.,!?()'+:]+$";

    /** Allowed MIME types for document uploads. */
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    /** Allowed file extensions for document uploads. */
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png"
    );
}
