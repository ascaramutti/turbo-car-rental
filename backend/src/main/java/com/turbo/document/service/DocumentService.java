package com.turbo.document.service;

import com.turbo.document.dto.AdminDocumentResponse;
import com.turbo.document.dto.DocumentResponse;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;

import java.util.List;

public interface DocumentService {

    DocumentResponse uploadDocument(UploadDocumentCommand command);

    List<DocumentResponse> getMyDocuments(Long userId);

    DocumentResponse reuploadDocument(ReuploadDocumentCommand command);

    List<AdminDocumentResponse> getPendingDocuments();

    AdminDocumentResponse reviewDocument(ReviewDocumentCommand command);

    List<AdminDocumentResponse> getDocumentsByUser(Long userId);
}
