package com.turbo.document.controller.mapper;

import com.turbo.document.dto.ReviewDocumentRequest;
import com.turbo.document.service.command.ReviewDocumentCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminDocumentControllerMapper {

    @Mapping(source = "documentId", target = "documentId")
    @Mapping(source = "request.action", target = "action")
    @Mapping(source = "request.licenseClass", target = "licenseClass")
    @Mapping(source = "request.rejectionReason", target = "rejectionReason")
    @Mapping(source = "adminId", target = "adminId")
    ReviewDocumentCommand toReviewCommand(Long documentId, ReviewDocumentRequest request, Long adminId);
}
