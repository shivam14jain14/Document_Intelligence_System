package com.docint.dto.response;

import com.docint.entity.SyncJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SyncJobDTO(
        UUID id,
        String sourceType,
        String category,
        String status,
        int totalFiles,
        int processedFiles,
        int failedFiles,
        List<String> errors,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    public static SyncJobDTO from(SyncJob job) {
        return new SyncJobDTO(
                job.getId(),
                job.getSourceType().name(),
                job.getCategory(),
                job.getStatus().name(),
                job.getTotalFiles(),
                job.getProcessedFiles(),
                job.getFailedFiles(),
                job.getErrors(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
