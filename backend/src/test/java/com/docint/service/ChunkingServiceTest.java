package com.docint.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for ChunkingService. It has @Value fields (chunkSize, chunkOverlap) that
 * Spring would normally inject from config — in a unit test we set them directly with
 * ReflectionTestUtils, no Spring context needed.
 */
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        ReflectionTestUtils.setField(chunkingService, "chunkSize", 400);
        ReflectionTestUtils.setField(chunkingService, "chunkOverlap", 50);
    }

    @Test
    @DisplayName("blank text produces no chunks (guard clause)")
    void blankTextProducesNoChunks() {
        assertThat(chunkingService.chunk("", Map.of())).isEmpty();
        assertThat(chunkingService.chunk("   ", Map.of())).isEmpty();
        assertThat(chunkingService.chunk(null, Map.of())).isEmpty();
    }

    @Test
    @DisplayName("real text is chunked and every chunk carries base metadata + chunk_index")
    void chunksCarryMetadata() {
        // Build a reasonably long text so the splitter emits at least one chunk
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("This is sentence number ").append(i)
              .append(" about vendor penalties and contract clauses. ");
        }

        Map<String, Object> base = new HashMap<>();
        base.put("document_id", "doc-123");
        base.put("category", "Legal");

        List<Document> chunks = chunkingService.chunk(sb.toString(), base);

        assertThat(chunks).isNotEmpty();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertThat(chunk.getText()).isNotBlank();
            assertThat(chunk.getMetadata()).containsEntry("document_id", "doc-123");
            assertThat(chunk.getMetadata()).containsEntry("category", "Legal");
            assertThat(chunk.getMetadata()).containsEntry("chunk_index", i);
        }
    }
}
