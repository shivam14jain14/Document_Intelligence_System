package com.docint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Document.StorageSource sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_config", columnDefinition = "jsonb")
    private Map<String, String> sourceConfig;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    @Column(name = "total_files")
    @Builder.Default
    private int totalFiles = 0;

    @Column(name = "processed_files")
    @Builder.Default
    private int processedFiles = 0;

    @Column(name = "failed_files")
    @Builder.Default
    private int failedFiles = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void incrementProcessed() {
        this.processedFiles++;
    }

    public void incrementFailed(String error) {
        this.failedFiles++;
        this.errors.add(error);
    }

    public enum SyncStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
