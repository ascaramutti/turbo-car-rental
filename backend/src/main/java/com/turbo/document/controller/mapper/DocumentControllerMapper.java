package com.turbo.document.controller.mapper;

import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.model.Document;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentControllerMapper {

    @Mapping(source = "file", target = "file")
    @Mapping(source = "documentType", target = "documentType")
    @Mapping(source = "userId", target = "userId")
    UploadDocumentCommand toUploadCommand(MultipartFile file, String documentType, Long userId);

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "file", target = "file")
    @Mapping(source = "userId", target = "userId")
    ReuploadDocumentCommand toReuploadCommand(Long documentId, MultipartFile file, Long userId);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(target = "documentType", expression = "java(document.getDocumentType().name())")
    @Mapping(target = "status", expression = "java(document.getStatus().name())")
    @Mapping(target = "uploadedAt", expression = "java(document.getUploadedAt() != null ? document.getUploadedAt().toString() : null)")
    @Mapping(target = "reviewedAt", expression = "java(document.getReviewedAt() != null ? document.getReviewedAt().toString() : null)")
    DocumentResponse toDocumentResponse(Document document);

    List<DocumentResponse> toDocumentResponseList(List<Document> documents);
}
