package com.docint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StorageSource source;

    @Column(name = "source_path", nullable = false)
    private String sourcePath;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_hash", length = 64, unique = true)
    private String fileHash;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(name = "error_message")
    private String errorMessage;

    /** Fine-grained stage while status=PROCESSING: PARSING, CHUNKING, EMBEDDING, DONE. */
    @Column(name = "processing_stage", length = 30)
    private String processingStage;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum StorageSource {
        LOCAL, S3, AZURE_BLOB, SHAREPOINT
    }

    public enum DocumentStatus {
        PROCESSING, INDEXED, FAILED, NEEDS_OCR
    }
}
