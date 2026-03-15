package com.turbo.document.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, Long userId, String documentType);

    Resource load(String filePath);

    void delete(String filePath);
}
