package com.turbo.exception.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode implements ErrorCode {

    INVALID_FILE_FORMAT("DOC-001", "Invalid file format. Only PDF, JPG, JPEG, and PNG are allowed", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("DOC-002", "File size exceeds the maximum allowed size of 5MB", HttpStatus.BAD_REQUEST),
    INVALID_DOCUMENT_TYPE("DOC-003", "Invalid document type. Only DRIVERS_LICENSE and STUDY_PERMIT are allowed", HttpStatus.BAD_REQUEST),
    DOCUMENT_ALREADY_EXISTS("DOC-004", "A document of this type is already pending or approved", HttpStatus.CONFLICT),
    DOCUMENT_ALREADY_APPROVED("DOC-005", "Approved documents cannot be replaced", HttpStatus.BAD_REQUEST),
    DOCUMENT_ACCESS_DENIED("DOC-006", "You do not have permission to modify this document", HttpStatus.FORBIDDEN),
    DOCUMENT_NOT_FOUND("DOC-007", "Document not found", HttpStatus.NOT_FOUND),
    REJECTION_REASON_REQUIRED("DOC-008", "Rejection reason is required when rejecting a document", HttpStatus.BAD_REQUEST),
    LICENSE_CLASS_REQUIRED("DOC-009", "License class is required when approving a driver's license", HttpStatus.BAD_REQUEST),
    DOCUMENT_NOT_PENDING("DOC-010", "Only documents with PENDING status can be reviewed", HttpStatus.BAD_REQUEST),
    NOT_A_DRIVER("DOC-011", "Only drivers can upload documents in this module", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
