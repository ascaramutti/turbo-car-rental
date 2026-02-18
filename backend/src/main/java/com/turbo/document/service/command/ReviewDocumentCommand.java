package com.turbo.document.service.command;

import lombok.Data;

@Data
public class ReviewDocumentCommand {

    private Long documentId;
    private String action;
    private String licenseClass;
    private String rejectionReason;
    private Long adminId;
}
