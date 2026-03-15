package com.turbo.document.controller.mapper;

import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.multipart.MultipartFile;

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
}
