package com.turbo.document.util;

import com.turbo.document.model.Document;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileResponseHelper {

    private static final String INLINE_DISPOSITION_TEMPLATE = "inline; filename=\"%s\"";
    private static final String ATTACHMENT_DISPOSITION_TEMPLATE = "attachment; filename=\"%s\"";
    private static final String SAFE_FILENAME_PATTERN = "[^a-zA-Z0-9._-]";
    private static final String SAFE_FILENAME_REPLACEMENT = "_";
    private static final String DEFAULT_FILENAME = "document";

    /** Maps file extensions to their real MIME types for inline preview. */
    private static final Map<String, MediaType> EXTENSION_MEDIA_TYPES = Map.of(
            "pdf", MediaType.APPLICATION_PDF,
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG
    );

    /** Builds an inline preview response with the correct Content-Type for the file extension. */
    public static ResponseEntity<Resource> buildViewResponse(Document document, Resource resource) {
        String safeFileName = sanitizeFileName(document.getFileName());
        MediaType mediaType = resolveMediaType(document.getFileName());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format(INLINE_DISPOSITION_TEMPLATE, safeFileName))
                .body(resource);
    }

    /** Builds a download response that forces the browser to download the file. */
    public static ResponseEntity<Resource> buildDownloadResponse(Document document, Resource resource) {
        String safeFileName = sanitizeFileName(document.getFileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format(ATTACHMENT_DISPOSITION_TEMPLATE, safeFileName))
                .body(resource);
    }

    /** Resolves the MediaType from the file extension, defaults to octet-stream. */
    private static MediaType resolveMediaType(String fileName) {
        if (fileName == null || !fileName.contains(".")) return MediaType.APPLICATION_OCTET_STREAM;
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return EXTENSION_MEDIA_TYPES.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);
    }

    /** Removes potentially dangerous characters from the filename. */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return DEFAULT_FILENAME;
        return fileName.replaceAll(SAFE_FILENAME_PATTERN, SAFE_FILENAME_REPLACEMENT);
    }
}
