package com.docint.dto.response;

import com.docint.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueryHistoryDTO(
        UUID messageId,
        UUID sessionId,
        String sessionTitle,
        String question,
        LocalDateTime askedAt
) {
    public static QueryHistoryDTO from(ChatMessage m) {
        return new QueryHistoryDTO(
                m.getId(),
                m.getSession().getId(),
                m.getSession().getTitle(),
                m.getContent(),
                m.getCreatedAt());
    }
}
