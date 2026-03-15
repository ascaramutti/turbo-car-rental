package com.turbo.document.util;

import com.turbo.document.fixture.DocumentFixture;
import com.turbo.document.model.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileResponseHelper")
class FileResponseHelperTest {

    private final Resource resource = new ByteArrayResource("test-content".getBytes());

    // ── buildViewResponse ────────────────────────────────────────────

    @Nested
    @DisplayName("buildViewResponse")
    class BuildViewResponse {

        @Test
        @DisplayName("PDF file returns application/pdf content type")
        void viewResponse_pdf_returnsCorrectContentType() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        }

        @Test
        @DisplayName("JPG file returns image/jpeg content type")
        void viewResponse_jpg_returnsImageJpeg() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("photo.jpg");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
        }

        @Test
        @DisplayName("PNG file returns image/png content type")
        void viewResponse_png_returnsImagePng() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("scan.png");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        }

        @Test
        @DisplayName("Unknown extension defaults to octet-stream")
        void viewResponse_unknownExtension_defaultsOctetStream() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("file.xyz");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }

        @Test
        @DisplayName("Sets inline Content-Disposition")
        void viewResponse_setsInlineDisposition() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).startsWith("inline;");
        }
    }

    // ── buildDownloadResponse ────────────────────────────────────────

    @Nested
    @DisplayName("buildDownloadResponse")
    class BuildDownloadResponse {

        @Test
        @DisplayName("Returns octet-stream content type (forces download)")
        void downloadResponse_returnsOctetStream() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildDownloadResponse(doc, resource);

            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }

        @Test
        @DisplayName("Sets attachment Content-Disposition")
        void downloadResponse_setsAttachmentDisposition() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildDownloadResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).startsWith("attachment;");
            assertThat(disposition).contains("license.pdf");
        }
    }

    // ── Filename sanitization (shared by both methods) ───────────────

    @Nested
    @DisplayName("Filename sanitization")
    class Sanitization {

        @Test
        @DisplayName("Sanitizes header injection attempt")
        void sanitizes_headerInjection() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license\"; evil=hack.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("inline; filename=\"license___evil_hack.pdf\"");
        }

        @Test
        @DisplayName("Sanitizes spaces and parentheses")
        void sanitizes_spacesAndParentheses() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("my license (2).pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildDownloadResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("attachment; filename=\"my_license__2_.pdf\"");
        }

        @Test
        @DisplayName("Null filename defaults to 'document'")
        void sanitizes_nullFilename() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName(null);

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).contains("document");
        }

        @Test
        @DisplayName("Returns resource as body")
        void returnsResourceBody() {
            Document doc = DocumentFixture.pendingLicense();

            ResponseEntity<Resource> response = FileResponseHelper.buildViewResponse(doc, resource);

            assertThat(response.getBody()).isEqualTo(resource);
        }
    }
}
