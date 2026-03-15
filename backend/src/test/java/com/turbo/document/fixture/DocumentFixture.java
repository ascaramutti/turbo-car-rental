package com.turbo.document.fixture;

import com.turbo.common.TestUtil;
import com.turbo.document.dto.ReviewDocumentRequest;
import com.turbo.document.model.Document;
import com.turbo.document.model.enums.DocumentStatus;
import com.turbo.document.model.enums.DocumentType;
import com.turbo.document.service.command.ReviewDocumentCommand;
import com.turbo.user.model.Admin;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;

public final class DocumentFixture {

    private static final String FIXTURES_PATH = "fixtures/document/";

    public static final Long DRIVER_USER_ID = 10L;
    public static final Long ADMIN_USER_ID = 1L;
    public static final Long DOCUMENT_ID = 100L;
    public static final String STORED_FILE_URL = "uploads/10/drivers_license_uuid.pdf";

    private DocumentFixture() {}

    // ── Users ────────────────────────────────────────────────────────

    /** Creates a Driver with default test values. */
    public static Driver testDriver() {
        Driver driver = new Driver();
        driver.setUserId(DRIVER_USER_ID);
        driver.setEmail("driver@test.com");
        driver.setFirstName("John");
        driver.setLastName("Doe");
        driver.setRole(UserRole.DRIVER);
        driver.setEmailVerified(true);
        driver.setIsVerified(false);
        driver.setRating(0.0f);
        driver.setIsWorkEligible(false);
        return driver;
    }

    /** Creates a CarOwner (not a Driver — used for DOC-011 tests). */
    public static CarOwner testCarOwner() {
        CarOwner owner = new CarOwner();
        owner.setUserId(20L);
        owner.setEmail("owner@test.com");
        owner.setFirstName("Sarah");
        owner.setLastName("Smith");
        owner.setRole(UserRole.CAR_OWNER);
        owner.setEmailVerified(true);
        owner.setRating(0.0f);
        return owner;
    }

    /** Creates an Admin user. */
    public static Admin testAdmin() {
        Admin admin = new Admin();
        admin.setUserId(ADMIN_USER_ID);
        admin.setEmail("admin@turbo.com");
        admin.setFirstName("Turbo");
        admin.setLastName("Admin");
        admin.setRole(UserRole.ADMIN);
        admin.setEmailVerified(true);
        admin.setIsVerified(true);
        admin.setDepartment("Platform Management");
        return admin;
    }

    // ── Documents ────────────────────────────────────────────────────

    /** Creates a PENDING DRIVERS_LICENSE document. */
    public static Document pendingLicense() {
        return buildDocument(DocumentType.DRIVERS_LICENSE, DocumentStatus.PENDING);
    }

    /** Creates a PENDING STUDY_PERMIT document. */
    public static Document pendingStudyPermit() {
        return buildDocument(DocumentType.STUDY_PERMIT, DocumentStatus.PENDING);
    }

    /** Creates an APPROVED DRIVERS_LICENSE document. */
    public static Document approvedLicense() {
        Document doc = buildDocument(DocumentType.DRIVERS_LICENSE, DocumentStatus.APPROVED);
        doc.setReviewedBy(ADMIN_USER_ID);
        doc.setReviewedAt(LocalDateTime.now());
        return doc;
    }

    /** Creates a REJECTED DRIVERS_LICENSE document. */
    public static Document rejectedLicense() {
        Document doc = buildDocument(DocumentType.DRIVERS_LICENSE, DocumentStatus.REJECTED);
        doc.setReviewedBy(ADMIN_USER_ID);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setRejectionReason("Document is blurry");
        return doc;
    }

    // ── Files ────────────────────────────────────────────────────────

    /** Creates a valid PDF MockMultipartFile under the 5MB limit. */
    public static MockMultipartFile validPdf() {
        return new MockMultipartFile("file", "license.pdf", "application/pdf", "pdf-content".getBytes());
    }

    /** Creates a valid JPG MockMultipartFile. */
    public static MockMultipartFile validJpg() {
        return new MockMultipartFile("file", "permit.jpg", "image/jpeg", "jpg-content".getBytes());
    }

    /** Creates a file with an unsupported format (.doc). */
    public static MockMultipartFile invalidFormatFile() {
        return new MockMultipartFile("file", "doc.docx", "application/msword", "doc-content".getBytes());
    }

    /** Creates a file exceeding the 5MB limit. */
    public static MockMultipartFile oversizedFile() {
        byte[] content = new byte[6 * 1024 * 1024];
        return new MockMultipartFile("file", "large.pdf", "application/pdf", content);
    }

    // ── Review requests from JSON ────────────────────────────────────

    public static ReviewDocumentRequest approveClass4Request() {
        return loadFixture("review-approve-class4-request.json", ReviewDocumentRequest.class);
    }

    public static ReviewDocumentRequest approveClass5Request() {
        return loadFixture("review-approve-class5-request.json", ReviewDocumentRequest.class);
    }

    public static ReviewDocumentRequest approvePermitRequest() {
        return loadFixture("review-approve-permit-request.json", ReviewDocumentRequest.class);
    }

    public static ReviewDocumentRequest rejectRequest() {
        return loadFixture("review-reject-request.json", ReviewDocumentRequest.class);
    }

    // ── Review commands ──────────────────────────────────────────────

    /** Builds a ReviewDocumentCommand from a request, including document and admin IDs. */
    public static ReviewDocumentCommand toCommand(ReviewDocumentRequest request) {
        ReviewDocumentCommand cmd = new ReviewDocumentCommand();
        cmd.setDocumentId(DOCUMENT_ID);
        cmd.setAction(request.getAction());
        cmd.setLicenseClass(request.getLicenseClass());
        cmd.setRejectionReason(request.getRejectionReason());
        cmd.setAdminId(ADMIN_USER_ID);
        return cmd;
    }

    // ── Internal ─────────────────────────────────────────────────────

    private static Document buildDocument(DocumentType type, DocumentStatus status) {
        Document doc = new Document();
        doc.setDocumentId(DOCUMENT_ID);
        doc.setUser(testDriver());
        doc.setDocumentType(type);
        doc.setFileUrl(STORED_FILE_URL);
        doc.setFileName("license.pdf");
        doc.setFileSize(2048576L);
        doc.setStatus(status);
        doc.setUploadedAt(LocalDateTime.now());
        return doc;
    }

    private static <T> T loadFixture(String fileName, Class<T> type) {
        return TestUtil.loadFixture(FIXTURES_PATH + fileName, type);
    }
}
