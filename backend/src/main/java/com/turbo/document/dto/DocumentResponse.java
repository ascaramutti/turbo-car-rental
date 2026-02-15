package com.turbo.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentResponse {

    private Long documentId;
    private Long userId;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private String status;
    private String uploadedAt;
    private String reviewedAt;
    private String rejectionReason;
}
