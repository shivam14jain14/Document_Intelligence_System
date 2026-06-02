package com.docint.config;

import com.docint.service.DocumentService;
import com.docint.service.VectorStoreService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.function.Function;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    // ── Tool: vector similarity search ──────────────────────────────────────
    public record VectorSearchRequest(String query, String category, int topK) {}
    public record ChunkResult(String content, String documentName, String documentId,
                              String category, int chunkIndex) {
        public static ChunkResult from(Document doc) {
            var meta = doc.getMetadata();
            return new ChunkResult(
                    doc.getText(),
                    (String) meta.getOrDefault("document_name", "Unknown"),
                    (String) meta.getOrDefault("document_id", ""),
                    (String) meta.getOrDefault("category", ""),
                    meta.get("chunk_index") instanceof Integer i ? i : 0
            );
        }
    }

    @Bean("vectorSearch")
    @org.springframework.context.annotation.Description(
            "Search the document knowledge base by semantic similarity. " +
            "Use this for every factual question. Call multiple times with different queries for complex questions. " +
            "Pass category to narrow the search (e.g. Legal, HR, Finance). Pass 'all' to search across all categories.")
    public Function<VectorSearchRequest, List<ChunkResult>> vectorSearch(VectorStoreService vectorStoreService) {
        return req -> {
            org.slf4j.LoggerFactory.getLogger(AiConfig.class)
                    .info(">>> Claude CALLED vectorSearch tool: query='{}' category='{}' topK={}",
                            req.query(), req.category(), req.topK());
            return vectorStoreService
                    .similaritySearch(req.query(), req.category(), req.topK() > 0 ? req.topK() : 5)
                    .stream().map(ChunkResult::from).toList();
        };
    }

    // ── Tool: document metadata ──────────────────────────────────────────────
    public record MetadataRequest(String documentId) {}
    public record DocumentMeta(String id, String name, String category, String source,
                               String fileType, String status, String createdAt) {}

    @Bean("getDocumentMetadata")
    @org.springframework.context.annotation.Description(
            "Get metadata for a specific document by its ID: filename, category, upload date, source system.")
    public Function<MetadataRequest, DocumentMeta> getDocumentMetadata(DocumentService documentService) {
        return req -> {
            try {
                var dto = documentService.getById(java.util.UUID.fromString(req.documentId()));
                return new DocumentMeta(dto.id().toString(), dto.name(), dto.category(),
                        dto.source(), dto.fileType(), dto.status(),
                        dto.createdAt() != null ? dto.createdAt().toString() : "");
            } catch (Exception e) {
                return new DocumentMeta(req.documentId(), "Unknown", "", "", "", "", "");
            }
        };
    }

    // ── Tool: list categories ────────────────────────────────────────────────
    public record CategoryListRequest(String unused) {}
    public record CategoryInfo(String name, long documentCount) {}

    @Bean("listCategories")
    @org.springframework.context.annotation.Description(
            "List all available document categories and their document counts. " +
            "Use this when the user asks what types of documents are available.")
    public Function<CategoryListRequest, List<CategoryInfo>> listCategories(DocumentService documentService) {
        return req -> documentService.getCategories()
                .stream().map(c -> new CategoryInfo(c.name(), c.documentCount())).toList();
    }
}
