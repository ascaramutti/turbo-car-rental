package com.turbo.document.service.command;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReuploadDocumentCommand {

    private Long documentId;
    private MultipartFile file;
    private Long userId;
}
