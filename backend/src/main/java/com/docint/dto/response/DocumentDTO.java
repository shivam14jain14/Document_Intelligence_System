package com.docint.dto.response;

import com.docint.entity.Document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentDTO(
        UUID id,
        String name,
        String category,
        String source,
        String fileType,
        Long fileSizeBytes,
        String status,
        String processingStage,
        String errorMessage,
        Integer pageCount,
        Integer chunkCount,
        LocalDateTime createdAt
) {
    public static DocumentDTO from(Document doc) {
        return new DocumentDTO(
                doc.getId(),
                doc.getName(),
                doc.getCategory(),
                doc.getSource().name(),
                doc.getFileType(),
                doc.getFileSizeBytes(),
                doc.getStatus().name(),
                doc.getProcessingStage(),
                doc.getErrorMessage(),
                doc.getPageCount(),
                doc.getChunkCount(),
                doc.getCreatedAt()
        );
    }
}
