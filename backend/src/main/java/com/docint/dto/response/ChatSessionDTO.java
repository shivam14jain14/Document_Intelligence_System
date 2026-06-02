package com.docint.dto.response;

import com.docint.entity.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSessionDTO(
        UUID id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChatSessionDTO from(ChatSession session) {
        return new ChatSessionDTO(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
