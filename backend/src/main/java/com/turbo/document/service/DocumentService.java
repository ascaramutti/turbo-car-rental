package com.turbo.document.service;

import com.turbo.document.model.Document;
import com.turbo.document.service.command.ReuploadDocumentCommand;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.document.service.command.UploadDocumentCommand;

import java.util.List;

public interface DocumentService {

    Document uploadDocument(UploadDocumentCommand command);

    List<Document> getMyDocuments(Long userId);

    Document reuploadDocument(ReuploadDocumentCommand command);

    List<Document> getPendingDocuments();

    Document reviewDocument(ReviewDocumentCommand command);

    List<Document> getDocumentsByUser(Long userId);

    Document getDocumentForDownload(Long documentId, Long userId);

    Document getDocumentForAdminView(Long documentId);
}
