package com.turbo.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDocumentResponse {

    private Long documentId;
    private Long userId;
    private String userFullName;
    private String userRole;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private String status;
    private String uploadedAt;
    private String reviewedAt;
    private Long reviewedBy;
    private String rejectionReason;
}
