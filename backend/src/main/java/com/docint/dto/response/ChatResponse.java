package com.docint.dto.response;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID messageId,
        UUID sessionId,
        String answer,
        List<SourceChunkDTO> sourceChunks
) {}
