package com.docint.repository;

import com.docint.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByFileHash(String fileHash);

    Optional<Document> findFirstByNameOrderByCreatedAtDesc(String name);

    Page<Document> findByCategoryAndStatus(String category, Document.DocumentStatus status, Pageable pageable);

    Page<Document> findByCategory(String category, Pageable pageable);

    Page<Document> findByStatus(Document.DocumentStatus status, Pageable pageable);

    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.status = 'INDEXED' GROUP BY d.category ORDER BY d.category")
    List<Object[]> countByCategory();

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Document d SET d.status = :status, d.chunkCount = :chunkCount, d.processingStage = 'DONE', d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    void updateStatusAndChunkCount(@Param("id") UUID id,
                                   @Param("status") Document.DocumentStatus status,
                                   @Param("chunkCount") int chunkCount);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Document d SET d.processingStage = :stage, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    void updateStage(@Param("id") UUID id, @Param("stage") String stage);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Document d SET d.status = :status, d.errorMessage = :message, d.processingStage = NULL, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    void updateStatusWithError(@Param("id") UUID id,
                               @Param("status") Document.DocumentStatus status,
                               @Param("message") String message);
}
