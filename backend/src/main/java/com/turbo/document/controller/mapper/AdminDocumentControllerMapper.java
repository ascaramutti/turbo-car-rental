package com.turbo.document.controller.mapper;

import com.turbo.document.dto.AdminDocumentResponse;
import com.turbo.document.dto.ReviewDocumentRequest;
import com.turbo.document.model.Document;
import com.turbo.document.service.command.ReviewDocumentCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminDocumentControllerMapper {

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "request.action", target = "action")
    @Mapping(source = "request.licenseClass", target = "licenseClass")
    @Mapping(source = "request.rejectionReason", target = "rejectionReason")
    @Mapping(source = "adminId", target = "adminId")
    ReviewDocumentCommand toReviewCommand(Long documentId, ReviewDocumentRequest request, Long adminId);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(target = "userFullName", expression = "java(document.getUser().getFirstName() + \" \" + document.getUser().getLastName())")
    @Mapping(target = "userRole", expression = "java(document.getUser().getRole().name())")
    @Mapping(target = "documentType", expression = "java(document.getDocumentType().name())")
    @Mapping(target = "status", expression = "java(document.getStatus().name())")
    @Mapping(target = "uploadedAt", expression = "java(document.getUploadedAt() != null ? document.getUploadedAt().toString() : null)")
    @Mapping(target = "reviewedAt", expression = "java(document.getReviewedAt() != null ? document.getReviewedAt().toString() : null)")
    @Mapping(source = "vehicleId", target = "vehicleId")
    AdminDocumentResponse toAdminDocumentResponse(Document document);

    List<AdminDocumentResponse> toAdminDocumentResponseList(List<Document> documents);
}
