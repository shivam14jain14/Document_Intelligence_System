package com.docint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChunkingService {

    @Value("${app.ingestion.chunk-size:400}")
    private int chunkSize;

    @Value("${app.ingestion.chunk-overlap:50}")
    private int chunkOverlap;

    public List<Document> chunk(String text, Map<String, Object> baseMetadata) {
        if (text == null || text.isBlank()) return List.of();

        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        Document rawDoc = new Document(text, baseMetadata);
        List<Document> chunks = splitter.apply(List.of(rawDoc));

        List<Document> indexed = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> meta = new HashMap<>(chunks.get(i).getMetadata());
            meta.put("chunk_index", i);
            indexed.add(new Document(chunks.get(i).getText(), meta));
        }

        log.debug("Split text into {} chunks (size={}, overlap={})", indexed.size(), chunkSize, chunkOverlap);
        return indexed;
    }
}
