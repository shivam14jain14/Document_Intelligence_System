package com.docint.util;

import com.docint.exception.InvalidFileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test using Spring's MockMultipartFile (a fake uploaded file) — no real HTTP.
 * Demonstrates testing both happy paths and the exception paths (edge cases).
 */
class FileValidationUtilTest {

    @Test
    @DisplayName("maps MIME types to friendly file-type labels")
    void mapsMimeToFileType() {
        assertThat(FileValidationUtil.toFileType("application/pdf")).isEqualTo("PDF");
        assertThat(FileValidationUtil.toFileType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).isEqualTo("XLSX");
        assertThat(FileValidationUtil.toFileType("something/unknown")).isEqualTo("TXT");
    }

    @Test
    @DisplayName("rejects an empty file")
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        // assertThatThrownBy verifies the RIGHT exception is thrown for a bad input
        assertThatThrownBy(() -> FileValidationUtil.validate(empty))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("rejects a disallowed file type (e.g. .exe)")
    void rejectsDisallowedType() {
        MockMultipartFile exe = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "MZ...".getBytes());

        assertThatThrownBy(() -> FileValidationUtil.validate(exe))
                .isInstanceOf(InvalidFileException.class);
    }
}
