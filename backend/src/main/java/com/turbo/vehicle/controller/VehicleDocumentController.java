package com.turbo.vehicle.controller;

import com.turbo.config.SecurityHelper;
import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.model.Document;
import com.turbo.document.service.FileStorageService;
import com.turbo.document.util.FileResponseHelper;
import com.turbo.user.model.User;
import com.turbo.vehicle.controller.mapper.VehicleDocumentControllerMapper;
import com.turbo.vehicle.service.VehicleDocumentService;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/owner/vehicles/{vehicleId}/documents")
@RequiredArgsConstructor
public class VehicleDocumentController {

    private final VehicleDocumentService vehicleDocumentService;
    private final VehicleDocumentControllerMapper controllerMapper;
    private final FileStorageService fileStorageService;
    private final SecurityHelper securityHelper;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long vehicleId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {

        User user = securityHelper.getCurrentUser();
        Document document = vehicleDocumentService.uploadVehicleDocument(
                controllerMapper.toUploadCommand(vehicleId, user.getUserId(), file, documentType));
        return ResponseEntity.ok(controllerMapper.toDocumentResponse(document));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable Long vehicleId) {
        User user = securityHelper.getCurrentUser();
        List<Document> documents = vehicleDocumentService.getVehicleDocuments(
                controllerMapper.toGetDocumentsCommand(vehicleId, user.getUserId()));
        return ResponseEntity.ok(controllerMapper.toDocumentResponseList(documents));
    }

    @PutMapping("/{documentId}/reupload")
    public ResponseEntity<DocumentResponse> reuploadDocument(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {

        User user = securityHelper.getCurrentUser();
        Document document = vehicleDocumentService.reuploadVehicleDocument(
                controllerMapper.toReuploadCommand(documentId, vehicleId, user.getUserId(), file));
        return ResponseEntity.ok(controllerMapper.toDocumentResponse(document));
    }

    @GetMapping("/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId) {

        User user = securityHelper.getCurrentUser();
        ViewVehicleDocumentCommand command = controllerMapper.toViewDocumentCommand(documentId, vehicleId, user.getUserId());
        Document document = vehicleDocumentService.getDocumentForView(command);
        Resource resource = fileStorageService.load(document.getFileUrl());
        return FileResponseHelper.buildViewResponse(document, resource);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long vehicleId,
            @PathVariable Long documentId) {

        User user = securityHelper.getCurrentUser();
        ViewVehicleDocumentCommand command = controllerMapper.toViewDocumentCommand(documentId, vehicleId, user.getUserId());
        Document document = vehicleDocumentService.getDocumentForDownload(command);
        Resource resource = fileStorageService.load(document.getFileUrl());
        return FileResponseHelper.buildDownloadResponse(document, resource);
    }
}
