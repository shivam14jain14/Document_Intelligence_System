package com.docint.repository;

import com.docint.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastNMessages(@Param("sessionId") UUID sessionId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.session.user.id = :userId AND m.role = 'USER' ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentUserQuestions(@Param("userId") UUID userId, Pageable pageable);
}
