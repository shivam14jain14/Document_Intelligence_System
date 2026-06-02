package com.docint.service;

import com.docint.entity.Document;
import com.docint.entity.DocumentChunk;
import com.docint.repository.DocumentChunkRepository;
import com.docint.repository.DocumentRepository;
import com.docint.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Runs the heavy ingestion pipeline (parse → chunk → embed → index) on a background thread.
 *
 * This lives in its OWN bean (not inside IngestionService) on purpose: Spring's {@code @Async}
 * only works when the method is invoked through the Spring proxy. An internal self-call
 * (IngestionService calling its own @Async method) bypasses the proxy and runs SYNCHRONOUSLY —
 * which would block the upload request until the whole pipeline finished. Calling this
 * separate bean is an external call, so @Async genuinely runs in the background and the
 * upload endpoint returns 202 immediately.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionProcessor {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentParserService parserService;
    private final ChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final com.docint.config.StorageConfig.StorageResolver storageResolver;

    @Async("ingestionExecutor")
    public CompletableFuture<Void> process(UUID documentId, byte[] fileBytes,
                                           String filename, String category,
                                           Document.StorageSource source) {
        log.info("Starting async ingestion for document: {}", documentId);
        try {
            // Stage 1: PARSING
            documentRepository.updateStage(documentId, "PARSING");
            DocumentParserService.ParsedDocument parsed =
                    parserService.parse(new ByteArrayInputStream(fileBytes), filename);

            if (parsed.isScanned()) {
                documentRepository.updateStatusWithError(documentId,
                        Document.DocumentStatus.NEEDS_OCR,
                        "Document appears to be scanned (no extractable text). OCR required.");
                return CompletableFuture.completedFuture(null);
            }

            Map<String, Object> baseMetadata = new HashMap<>();
            baseMetadata.put("document_id", documentId.toString());
            baseMetadata.put("document_name", filename);
            baseMetadata.put("category", category);
            baseMetadata.put("source", source.name());

            // Stage 2: CHUNKING
            documentRepository.updateStage(documentId, "CHUNKING");
            List<org.springframework.ai.document.Document> chunks =
                    chunkingService.chunk(parsed.text(), baseMetadata);

            // Stage 3: EMBEDDING (saveChunks embeds via OpenAI then writes to pgvector)
            documentRepository.updateStage(documentId, "EMBEDDING");
            vectorStoreService.saveChunks(chunks);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                var chunk = chunks.get(i);
                Document docRef = new Document();
                docRef.setId(documentId);
                chunkEntities.add(DocumentChunk.builder()
                        .document(docRef)
                        .chunkIndex(i)
                        .content(chunk.getText())
                        .tokenCount(chunk.getText().split("\\s+").length)
                        .build());
            }
            documentChunkRepository.saveAll(chunkEntities);

            documentRepository.updateStatusAndChunkCount(
                    documentId, Document.DocumentStatus.INDEXED, chunks.size());

            log.info("Ingestion complete for {}: {} chunks indexed", filename, chunks.size());

        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", documentId, e.getMessage(), e);
            documentRepository.updateStatusWithError(documentId,
                    Document.DocumentStatus.FAILED, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("ingestionExecutor")
    public CompletableFuture<Void> processStored(UUID documentId, String filename,
                                                 String category, Document.StorageSource source) {
        log.info("Starting async stored-object ingestion for document: {}", documentId);
        try {
            Document storedDoc = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalStateException("Document not found during stored-object ingestion: " + documentId));
            StorageService storage = storageResolver.resolve(source.name());

            documentRepository.updateStage(documentId, "PARSING");
            DocumentParserService.ParsedDocument parsed;
            try (InputStream stream = storage.download(storedDoc.getSourcePath())) {
                parsed = parserService.parse(stream, filename);
            }

            if (parsed.isScanned()) {
                documentRepository.updateStatusWithError(documentId,
                        Document.DocumentStatus.NEEDS_OCR,
                        "Document appears to be scanned (no extractable text). OCR required.");
                return CompletableFuture.completedFuture(null);
            }

            Map<String, Object> baseMetadata = new HashMap<>();
            baseMetadata.put("document_id", documentId.toString());
            baseMetadata.put("document_name", filename);
            baseMetadata.put("category", category);
            baseMetadata.put("source", source.name());

            documentRepository.updateStage(documentId, "CHUNKING");
            List<org.springframework.ai.document.Document> chunks =
                    chunkingService.chunk(parsed.text(), baseMetadata);

            documentRepository.updateStage(documentId, "EMBEDDING");
            vectorStoreService.saveChunks(chunks);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                var chunk = chunks.get(i);
                Document docRef = new Document();
                docRef.setId(documentId);
                chunkEntities.add(DocumentChunk.builder()
                        .document(docRef)
                        .chunkIndex(i)
                        .content(chunk.getText())
                        .tokenCount(chunk.getText().split("\\s+").length)
                        .build());
            }
            documentChunkRepository.saveAll(chunkEntities);
            documentRepository.updateStatusAndChunkCount(
                    documentId, Document.DocumentStatus.INDEXED, chunks.size());
            log.info("Stored-object ingestion complete for {}: {} chunks indexed", filename, chunks.size());
        } catch (Exception e) {
            log.error("Stored-object ingestion failed for document {}: {}", documentId, e.getMessage(), e);
            documentRepository.updateStatusWithError(documentId,
                    Document.DocumentStatus.FAILED, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}
