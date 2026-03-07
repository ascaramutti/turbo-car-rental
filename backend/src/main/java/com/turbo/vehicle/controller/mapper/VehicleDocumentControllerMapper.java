package com.turbo.vehicle.controller.mapper;

import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.model.Document;
import com.turbo.vehicle.service.command.GetVehicleDocumentsCommand;
import com.turbo.vehicle.service.command.ReuploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.UploadVehicleDocumentCommand;
import com.turbo.vehicle.service.command.ViewVehicleDocumentCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleDocumentControllerMapper {

    // ── Request → Command mappings ──────────────────────────────────

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    @Mapping(source = "file", target = "file")
    @Mapping(source = "documentType", target = "documentType")
    UploadVehicleDocumentCommand toUploadCommand(Long vehicleId, Long ownerId, MultipartFile file, String documentType);

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    @Mapping(source = "file", target = "file")
    ReuploadVehicleDocumentCommand toReuploadCommand(Long documentId, Long vehicleId, Long ownerId, MultipartFile file);

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    GetVehicleDocumentsCommand toGetDocumentsCommand(Long vehicleId, Long ownerId);

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "ownerId", target = "ownerId")
    ViewVehicleDocumentCommand toViewDocumentCommand(Long documentId, Long vehicleId, Long ownerId);

    // ── Entity → Response mappings ──────────────────────────────────

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(target = "documentType", expression = "java(document.getDocumentType().name())")
    @Mapping(target = "status", expression = "java(document.getStatus().name())")
    @Mapping(target = "uploadedAt", expression = "java(document.getUploadedAt() != null ? document.getUploadedAt().toString() : null)")
    @Mapping(target = "reviewedAt", expression = "java(document.getReviewedAt() != null ? document.getReviewedAt().toString() : null)")
    DocumentResponse toDocumentResponse(Document document);

    List<DocumentResponse> toDocumentResponseList(List<Document> documents);
}
