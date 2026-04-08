package com.turbo.document.service.mapper;

import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.model.enums.LicenseClass;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.web.multipart.MultipartFile;

@Mapper(componentModel = "spring")
public interface DocumentServiceMapper {

    // ── Upload → Entity mapping ─────────────────────────────────────

    /** Builds a new Document entity from upload data. */
    @Mapping(source = "user", target = "user")
    @Mapping(source = "documentType", target = "documentType")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(target = "fileName", expression = "java(file.getOriginalFilename())")
    @Mapping(target = "fileSize", expression = "java(file.getSize())")
    @Mapping(target = "status", expression = "java(com.turbo.document.model.enums.DocumentStatus.PENDING)")
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    Document toNewDocument(User user, DocumentType documentType, String fileUrl, MultipartFile file);

    /** Resets a document's fields for re-review after a reupload. */
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(target = "fileName", expression = "java(file.getOriginalFilename())")
    @Mapping(target = "fileSize", expression = "java(file.getSize())")
    @Mapping(target = "status", expression = "java(com.turbo.document.model.enums.DocumentStatus.PENDING)")
    @Mapping(target = "rejectionReason", expression = "java(null)")
    @Mapping(target = "reviewedAt", expression = "java(null)")
    @Mapping(target = "reviewedBy", expression = "java(null)")
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    void updateDocumentForReupload(String fileUrl, MultipartFile file, @MappingTarget Document document);

    /** Applies review metadata (admin ID and timestamp) to a document. */
    @Mapping(source = "adminId", target = "reviewedBy")
    @Mapping(target = "reviewedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "fileUrl", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    void applyReviewMetadata(Long adminId, @MappingTarget Document document);

    /** Applies license approval fields to the driver entity. */
    default void applyLicenseApproval(LicenseClass licenseClass, Driver driver) {
        driver.setLicenseClass(licenseClass.name());
        driver.setIsWorkEligible(licenseClass == LicenseClass.CLASS_4);
        driver.setCheckStatus(DocumentStatus.APPROVED.name());
    }

    /** Sets document status to APPROVED. */
    default void applyApproval(Document document) {
        document.setStatus(DocumentStatus.APPROVED);
    }

    /** Sets document status to REJECTED with the provided reason. */
    default void applyRejection(String rejectionReason, Document document) {
        document.setStatus(DocumentStatus.REJECTED);
        document.setRejectionReason(rejectionReason);
    }
}
