package com.turbo.document.service.impl;

import com.turbo.document.fixture.DocumentFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageServiceImpl")
class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", tempDir.toString());
    }

    // ── store ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("Stores file and returns valid path")
        void store_validFile_returnsPath() {
            String result = fileStorageService.store(DocumentFixture.validPdf(), 10L, "DRIVERS_LICENSE");

            assertThat(result).contains("10").contains("drivers_license").endsWith(".pdf");
            assertThat(Path.of(result)).exists();
        }

        @Test
        @DisplayName("Creates user subdirectory if not exists")
        void store_createsUserDirectory() {
            fileStorageService.store(DocumentFixture.validPdf(), 42L, "STUDY_PERMIT");

            assertThat(tempDir.resolve("42")).isDirectory();
        }

        @Test
        @DisplayName("Generates unique filenames for consecutive uploads")
        void store_uniqueFileNames() {
            String path1 = fileStorageService.store(DocumentFixture.validPdf(), 10L, "DRIVERS_LICENSE");
            String path2 = fileStorageService.store(DocumentFixture.validPdf(), 10L, "DRIVERS_LICENSE");

            assertThat(path1).isNotEqualTo(path2);
        }

        @Test
        @DisplayName("Null filename defaults to .bin extension")
        void store_nullFilename_defaultsTobin() {
            MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf", "content".getBytes());

            String result = fileStorageService.store(file, 10L, "DRIVERS_LICENSE");

            assertThat(result).endsWith(".bin");
        }

        @Test
        @DisplayName("Filename without extension defaults to .bin")
        void store_noExtension_defaultsToBin() {
            MockMultipartFile file = new MockMultipartFile("file", "noext", "application/pdf", "content".getBytes());

            String result = fileStorageService.store(file, 10L, "DRIVERS_LICENSE");

            assertThat(result).endsWith(".bin");
        }
    }

    // ── init ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("Creates upload directory on startup")
        void init_createsDirectory() {
            Path newDir = tempDir.resolve("uploads-init");
            ReflectionTestUtils.setField(fileStorageService, "uploadDir", newDir.toString());

            fileStorageService.init();

            assertThat(newDir).isDirectory();
        }
    }

    // ── load ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("Loads existing file as Resource")
        void load_existingFile_returnsResource() throws IOException {
            Path file = tempDir.resolve("test.pdf");
            Files.write(file, "pdf-content".getBytes());

            org.springframework.core.io.Resource resource = fileStorageService.load(file.toString());

            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
        }

        @Test
        @DisplayName("Throws for non-existent file")
        void load_nonExistentFile_throwsException() {
            String fakePath = tempDir.resolve("nonexistent.pdf").toString();

            assertThatThrownBy(() -> fileStorageService.load(fakePath))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found or not readable");
        }
    }

    // ── delete ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Deletes existing file")
        void delete_existingFile_removesIt() throws IOException {
            Path file = tempDir.resolve("test.pdf");
            Files.write(file, "content".getBytes());
            assertThat(file).exists();

            fileStorageService.delete(file.toString());

            assertThat(file).doesNotExist();
        }

        @Test
        @DisplayName("Does not throw for non-existent file")
        void delete_nonExistentFile_noException() {
            fileStorageService.delete(tempDir.resolve("nonexistent.pdf").toString());
        }
    }
}
