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

    @Nested
    @DisplayName("buildFileResponse")
    class BuildFileResponse {

        @Test
        @DisplayName("Returns 200 with correct content type")
        void buildFileResponse_correctContentType() {
            Document doc = DocumentFixture.pendingLicense();

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        }

        @Test
        @DisplayName("Sets Content-Disposition with inline and filename")
        void buildFileResponse_setsContentDisposition() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("inline; filename=\"license.pdf\"");
        }

        @Test
        @DisplayName("Sanitizes filename with header injection attempt")
        void buildFileResponse_sanitizesHeaderInjection() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("license\"; evil=hack.pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("inline; filename=\"license___evil_hack.pdf\"");
        }

        @Test
        @DisplayName("Sanitizes filename with spaces and parentheses")
        void buildFileResponse_sanitizesSpacesAndParentheses() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName("my license (2).pdf");

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("inline; filename=\"my_license__2_.pdf\"");
        }

        @Test
        @DisplayName("Null filename defaults to 'document'")
        void buildFileResponse_nullFilename_defaults() {
            Document doc = DocumentFixture.pendingLicense();
            doc.setFileName(null);

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            assertThat(disposition).isEqualTo("inline; filename=\"document\"");
        }

        @Test
        @DisplayName("Returns the resource as body")
        void buildFileResponse_returnsResourceBody() {
            Document doc = DocumentFixture.pendingLicense();

            ResponseEntity<Resource> response = FileResponseHelper.buildFileResponse(doc, resource);

            assertThat(response.getBody()).isEqualTo(resource);
        }
    }
}
