package com.turbo.document.util;

import com.turbo.document.model.Document;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileResponseHelper {

    private static final String CONTENT_DISPOSITION_TEMPLATE = "inline; filename=\"%s\"";
    private static final String SAFE_FILENAME_PATTERN = "[^a-zA-Z0-9._-]";
    private static final String SAFE_FILENAME_REPLACEMENT = "_";

    /** Builds a file download ResponseEntity with sanitized Content-Disposition header. */
    public static ResponseEntity<Resource> buildFileResponse(Document document, Resource resource) {
        String safeFileName = sanitizeFileName(document.getFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format(CONTENT_DISPOSITION_TEMPLATE, safeFileName))
                .body(resource);
    }

    /** Removes potentially dangerous characters from the filename. */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        return fileName.replaceAll(SAFE_FILENAME_PATTERN, SAFE_FILENAME_REPLACEMENT);
    }
}
