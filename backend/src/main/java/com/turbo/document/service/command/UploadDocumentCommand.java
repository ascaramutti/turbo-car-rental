package com.turbo.document.service.command;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadDocumentCommand {

    private MultipartFile file;
    private String documentType;
    private Long userId;
}
