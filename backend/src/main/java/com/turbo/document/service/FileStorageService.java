package com.turbo.document.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /** Stores the file and returns the relative path where it was saved. */
    String store(MultipartFile file, Long userId, String documentType);

    /** Deletes the file at the given relative path. */
    void delete(String filePath);
}
