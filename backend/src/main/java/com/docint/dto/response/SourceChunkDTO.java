package com.docint.dto.response;

public record SourceChunkDTO(
        String documentId,
        String documentName,
        String category,
        String excerpt
) {}
