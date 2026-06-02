package com.docint.dto.response;

import com.docint.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatMessageDTO(
        UUID id,
        String role,
        String content,
        List<SourceChunkDTO> sourceChunks,
        LocalDateTime createdAt
) {
    public static ChatMessageDTO from(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getId(),
                msg.getRole().name(),
                msg.getContent(),
                List.of(),
                msg.getCreatedAt()
        );
    }
}
