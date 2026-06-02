package com.docint.util;

import com.docint.exception.InvalidFileException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

public final class FileValidationUtil {

    private FileValidationUtil() {}

    private static final Tika TIKA = new Tika();

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
            "text/plain"
    );

    private static final long MAX_SIZE_BYTES = 100L * 1024 * 1024; // 100MB

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or missing");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidFileException("File size exceeds the 100MB limit");
        }
        String detectedMime = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new InvalidFileException(
                    "Unsupported file type: " + detectedMime +
                    ". Allowed types: PDF, DOCX, XLSX, PPTX, TXT");
        }
    }

    public static String detectMimeType(MultipartFile file) {
        try {
            return TIKA.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new InvalidFileException("Could not read file to detect type");
        }
    }

    public static String toFileType(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> "PDF";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/msword" -> "DOCX";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.ms-excel" -> "XLSX";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                 "application/vnd.ms-powerpoint" -> "PPTX";
            default -> "TXT";
        };
    }
}
