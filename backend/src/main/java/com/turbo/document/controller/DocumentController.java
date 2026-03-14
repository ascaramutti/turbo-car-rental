package com.turbo.document.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.document.controller.mapper.DocumentControllerMapper;
import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.service.DocumentService;
import com.turbo.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/driver/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentControllerMapper controllerMapper;
    private final SecurityHelper securityHelper;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {

        User user = securityHelper.getCurrentUser();
        return ResponseEntity.ok(documentService.uploadDocument(controllerMapper.toUploadCommand(file, documentType, user.getUserId())));
    }

    @GetMapping("/my")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments() {
        User user = securityHelper.getCurrentUser();
        return ResponseEntity.ok(documentService.getMyDocuments(user.getUserId()));
    }

    @PutMapping("/{id}/reupload")
    public ResponseEntity<DocumentResponse> reuploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        User user = securityHelper.getCurrentUser();
        return ResponseEntity.ok(documentService.reuploadDocument(controllerMapper.toReuploadCommand(id, file, user.getUserId())));
    }
}
