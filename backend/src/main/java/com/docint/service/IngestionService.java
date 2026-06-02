package com.docint.service;

import com.docint.entity.Document;
import com.docint.entity.User;
import com.docint.exception.DocumentProcessingException;
import com.docint.repository.DocumentChunkRepository;
import com.docint.repository.DocumentRepository;
import com.docint.service.storage.StorageService;
import com.docint.util.FileValidationUtil;
import com.docint.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final StorageService storageService;
    private final com.docint.config.StorageConfig.StorageResolver storageResolver;
    private final IngestionProcessor ingestionProcessor;   // separate bean → real @Async
    private final VectorStoreService vectorStoreService;
    private final AuditService auditService;

    // NOT @Transactional: the document save must COMMIT before the async processor starts,
    // otherwise the background thread can't see the row (and chunk inserts would fail the FK).
    public Document upload(MultipartFile file, String category, String storageProvider, User uploadedBy) {
        FileValidationUtil.validate(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new DocumentProcessingException("Could not read uploaded file", e);
        }

        String fileHash = HashUtil.sha256(fileBytes);
        Optional<Document> existing = documentRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            log.info("Duplicate detected — returning existing document: {}", existing.get().getId());
            return existing.get();
        }

        Document.StorageSource source = parseSource(storageProvider);
        StorageService targetStorage = storageResolver.resolve(source.name());

        String mimeType = FileValidationUtil.detectMimeType(file);
        String fileType = FileValidationUtil.toFileType(mimeType);
        String docId = UUID.randomUUID().toString();
        String storageKey = "documents/" + category + "/" + docId + "/" + file.getOriginalFilename();

        targetStorage.upload(storageKey, new ByteArrayInputStream(fileBytes), fileBytes.length, mimeType);

        Document doc = Document.builder()
                .name(file.getOriginalFilename())
                .category(category)
                .source(source)
                .sourcePath(storageKey)
                .fileType(fileType)
                .fileSizeBytes(file.getSize())
                .fileHash(fileHash)
                .status(Document.DocumentStatus.PROCESSING)
                .uploadedBy(uploadedBy)
                .build();

        Document saved = documentRepository.save(doc);
        if (uploadedBy != null) {
            auditService.log(uploadedBy.getEmail(), "UPLOAD", file.getOriginalFilename(),
                    "category=" + category + ", storage=" + source);
        }
        // External call → @Async genuinely runs in the background; this returns 202 immediately
        ingestionProcessor.process(saved.getId(), fileBytes, file.getOriginalFilename(), category, source);
        return saved;
    }

    // NOT @Transactional — same reason as upload(): commit the doc before async processing.
    public Document ingestStream(InputStream stream, String filename, String category,
                                  Document.StorageSource source, User uploadedBy) {
        try {
            byte[] fileBytes = stream.readAllBytes();
            String fileHash = HashUtil.sha256(fileBytes);
            Optional<Document> existing = documentRepository.findByFileHash(fileHash);
            if (existing.isPresent()) return existing.get();

            String mimeType = new org.apache.tika.Tika().detect(fileBytes, filename);
            String fileType = FileValidationUtil.toFileType(mimeType);
            String docId = UUID.randomUUID().toString();
            String storageKey = "documents/" + category + "/" + docId + "/" + filename;

            storageService.upload(storageKey, new ByteArrayInputStream(fileBytes), fileBytes.length, mimeType);

            Document doc = Document.builder()
                    .name(filename).category(category).source(source)
                    .sourcePath(storageKey).fileType(fileType)
                    .fileSizeBytes((long) fileBytes.length).fileHash(fileHash)
                    .status(Document.DocumentStatus.PROCESSING).uploadedBy(uploadedBy)
                    .build();
            Document saved = documentRepository.save(doc);
            ingestionProcessor.process(saved.getId(), fileBytes, filename, category, source);
            return saved;
        } catch (java.io.IOException e) {
            throw new DocumentProcessingException("Failed to read stream for: " + filename, e);
        }
    }

    // NOT @Transactional â€” save must commit before async processor reads the row.
    public Document finalizeMultipartUpload(String filename, String category, String contentType,
                                           long fileSizeBytes, String storageKey, User uploadedBy) {
        Document.StorageSource source = Document.StorageSource.S3;
        StorageService s3Storage = storageResolver.resolve(source.name());

        String fileHash;
        try (InputStream stream = s3Storage.download(storageKey)) {
            fileHash = HashUtil.sha256(stream);
        } catch (IOException e) {
            throw new DocumentProcessingException("Could not close uploaded S3 object stream", e);
        } catch (Exception e) {
            throw new DocumentProcessingException("Could not read uploaded S3 object for hashing", e);
        }

        Optional<Document> existing = documentRepository.findByFileHash(fileHash);
        if (existing.isPresent()) {
            s3Storage.delete(storageKey);
            log.info("Duplicate multipart upload detected — returning existing document: {}", existing.get().getId());
            return existing.get();
        }

        String fileType = FileValidationUtil.toFileType(contentType);
        Document doc = Document.builder()
                .name(filename)
                .category(category)
                .source(source)
                .sourcePath(storageKey)
                .fileType(fileType)
                .fileSizeBytes(fileSizeBytes)
                .fileHash(fileHash)
                .status(Document.DocumentStatus.PROCESSING)
                .processingStage("UPLOADED")
                .uploadedBy(uploadedBy)
                .build();

        Document saved = documentRepository.save(doc);
        if (uploadedBy != null) {
            auditService.log(uploadedBy.getEmail(), "UPLOAD_MULTIPART", filename,
                    "category=" + category + ", storage=S3, size=" + fileSizeBytes);
        }
        ingestionProcessor.processStored(saved.getId(), filename, category, source);
        return saved;
    }

    private Document.StorageSource parseSource(String provider) {
        if (provider == null || provider.isBlank()) return Document.StorageSource.LOCAL;
        try {
            return Document.StorageSource.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Document.StorageSource.LOCAL;
        }
    }

    @Transactional
    public void delete(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new com.docint.exception.ResourceNotFoundException("Document not found: " + documentId));
        try {
            // Delete from the backend the document was actually stored in, not the default provider.
            storageResolver.resolve(doc.getSource().name()).delete(doc.getSourcePath());
        } catch (Exception e) {
            log.warn("Could not delete file from storage: {}", doc.getSourcePath());
        }
        vectorStoreService.deleteByDocumentId(documentId.toString());
        documentChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(doc);
    }
}
