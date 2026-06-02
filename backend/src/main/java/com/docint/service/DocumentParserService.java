package com.docint.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DocumentParserService {

    private final AutoDetectParser parser = new AutoDetectParser();

    public ParsedDocument parse(InputStream inputStream, String filename) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set("resourceName", filename);
        ParseContext context = new ParseContext();

        try {
            parser.parse(inputStream, handler, metadata, context);
            String text = handler.toString().trim();

            Map<String, String> meta = new HashMap<>();
            for (String name : metadata.names()) {
                meta.put(name, metadata.get(name));
            }

            String pageCountStr = metadata.get("xmpTPg:NPages");
            int pageCount = 0;
            if (pageCountStr != null) {
                try { pageCount = Integer.parseInt(pageCountStr); } catch (NumberFormatException ignored) {}
            }

            if (text.isEmpty()) {
                log.warn("No text extracted from: {} — may be a scanned document", filename);
                return new ParsedDocument("", meta, pageCount, true);
            }

            log.debug("Parsed {} — {} chars, {} pages", filename, text.length(), pageCount);
            return new ParsedDocument(text, meta, pageCount, false);

        } catch (Exception e) {
            log.error("Failed to parse document: {}", filename, e);
            throw new RuntimeException("Document parsing failed: " + e.getMessage(), e);
        }
    }

    public record ParsedDocument(
            String text,
            Map<String, String> tikaMetadata,
            int pageCount,
            boolean isScanned
    ) {}
}
