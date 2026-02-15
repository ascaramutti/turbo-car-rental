package com.turbo.document.repository;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUserUserId(Long userId);

    List<Document> findByStatus(DocumentStatus status);

    boolean existsByUserUserIdAndDocumentTypeAndStatusIn(Long userId, DocumentType documentType, List<DocumentStatus> statuses);

    List<Document> findByVehicleId(Long vehicleId);

    boolean existsByVehicleIdAndDocumentTypeAndStatusIn(Long vehicleId, DocumentType documentType, List<DocumentStatus> statuses);
}
