package com.docint.service;

import com.docint.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Legal", "HR", "Finance", "Security", "Engineering", "General", "Other"
    );

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.VECTOR_SEARCH, allEntries = true),
            @CacheEvict(value = CacheConfig.CHAT_ANSWERS, allEntries = true)
    })
    public void saveChunks(List<Document> chunks) {
        if (chunks.isEmpty()) return;
        vectorStore.add(chunks);
        log.debug("Saved {} chunks (vectorSearch + chatAnswers caches evicted)", chunks.size());
    }

    @Cacheable(value = CacheConfig.VECTOR_SEARCH,
            key = "#query + '|' + #category + '|' + #topK")
    public List<Document> similaritySearch(String query, String category, int topK) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK > 0 ? topK : 5)
                .similarityThreshold(0.0);  // rank by topK; do not hard-filter low scores

        if (StringUtils.hasText(category) && !category.equalsIgnoreCase("all")) {
            String safeCategory = sanitizeCategory(category);
            builder.filterExpression("category == '" + safeCategory + "'");
        }

        List<Document> results = vectorStore.similaritySearch(builder.build());
        log.info("[vectorSearch] query='{}' category='{}' topK={} -> {} results",
                query, category, topK, results.size());
        results.forEach(d -> log.info("   match (score={}): {}",
                d.getMetadata().get("distance"),
                d.getText().substring(0, Math.min(80, d.getText().length())).replaceAll("\\s+", " ")));
        return results;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.VECTOR_SEARCH, allEntries = true),
            @CacheEvict(value = CacheConfig.CHAT_ANSWERS, allEntries = true)
    })
    public void deleteByDocumentId(String documentId) {
        // PgVectorStore supports deletion by metadata filter in Spring AI 1.0.0+
        try {
            vectorStore.delete(List.of(documentId));
        } catch (Exception e) {
            log.warn("Could not delete vectors for document {}: {}", documentId, e.getMessage());
        }
    }

    private String sanitizeCategory(String category) {
        // Strip any characters that could break the filter expression
        return category.replaceAll("[^a-zA-Z0-9_\\- ]", "");
    }
}
