package com.turbo.document.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.document.controller.mapper.AdminDocumentControllerMapper;
import com.turbo.document.dto.AdminDocumentResponse;
import com.turbo.document.dto.ReviewDocumentRequest;
import com.turbo.document.model.Document;
import com.turbo.document.service.DocumentService;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.util.FileResponseHelper;
import com.turbo.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;
    private final AdminDocumentControllerMapper controllerMapper;
    private final FileStorageService fileStorageService;
    private final SecurityHelper securityHelper;

    @GetMapping("/pending")
    public ResponseEntity<List<AdminDocumentResponse>> getPendingDocuments() {
        return ResponseEntity.ok(documentService.getPendingDocuments());
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<AdminDocumentResponse> reviewDocument(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDocumentRequest request) {

        User admin = securityHelper.getCurrentUser();
        return ResponseEntity.ok(documentService.reviewDocument(controllerMapper.toReviewCommand(id, request, admin.getUserId())));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AdminDocumentResponse>> getDocumentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(documentService.getDocumentsByUser(userId));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> viewFile(@PathVariable Long id) {
        Document document = documentService.getDocumentForAdminView(id);
        Resource resource = fileStorageService.load(document.getFileUrl());
        return FileResponseHelper.buildFileResponse(document, resource);
    }
}
