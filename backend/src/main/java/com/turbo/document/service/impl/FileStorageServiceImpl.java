package com.turbo.document.service.impl;

import com.turbo.document.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final String DEFAULT_EXTENSION = "bin";
    private static final String ERROR_CREATE_DIRECTORY = "Could not create directory: ";
    private static final String ERROR_STORE_FILE = "Failed to store file: ";
    private static final String ERROR_LOAD_FILE = "Failed to load file: ";
    private static final String ERROR_FILE_NOT_READABLE = "File not found or not readable: ";

    @Value("${file.upload-dir}")
    private String uploadDir;

    /** Ensures the base upload directory exists on startup. */
    @PostConstruct
    protected void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException(ERROR_CREATE_DIRECTORY + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, Long userId, String documentType) {
        Path userDir = Paths.get(uploadDir, userId.toString());
        createDirectoryIfNotExists(userDir);

        String uniqueFileName = buildUniqueFileName(file.getOriginalFilename(), documentType);
        Path targetPath = userDir.resolve(uniqueFileName);

        copyFile(file, targetPath);
        return targetPath.toString();
    }

    @Override
    public Resource load(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException(ERROR_FILE_NOT_READABLE + filePath);
            }
            return resource;
        } catch (IOException e) {
            throw new RuntimeException(ERROR_LOAD_FILE + filePath, e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filePath, e);
        }
    }

    /** Creates the directory if it does not already exist. */
    private void createDirectoryIfNotExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(ERROR_CREATE_DIRECTORY + dir, e);
        }
    }

    /** Generates a unique file name using UUID + document type + original extension. */
    private String buildUniqueFileName(String originalFileName, String documentType) {
        String extension = extractExtension(originalFileName);
        return documentType.toLowerCase() + "_" + UUID.randomUUID() + "." + extension;
    }

    /** Extracts the file extension from the original file name. */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return DEFAULT_EXTENSION;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /** Copies the uploaded file to the target path. */
    private void copyFile(MultipartFile file, Path targetPath) {
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(ERROR_STORE_FILE + targetPath, e);
        }
    }
}
