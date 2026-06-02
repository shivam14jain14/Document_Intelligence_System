package com.docint.service;

import com.docint.dto.response.ChatMessageDTO;
import com.docint.dto.response.ChatSessionDTO;
import com.docint.entity.ChatMessage;
import com.docint.entity.ChatSession;
import com.docint.entity.User;
import com.docint.exception.ResourceNotFoundException;
import com.docint.repository.ChatMessageRepository;
import com.docint.repository.ChatSessionRepository;
import com.docint.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionDTO createSession(String userEmail, String title) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        ChatSession session = ChatSession.builder()
                .user(user)
                .title(title != null && !title.isBlank() ? title : "New Chat")
                .build();
        return ChatSessionDTO.from(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<ChatSessionDTO> listSessions(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return List.of();
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream().map(ChatSessionDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getHistory(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(ChatMessageDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<com.docint.dto.response.QueryHistoryDTO> getQueryHistory(String userEmail, int limit) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return List.of();
        return messageRepository.findRecentUserQuestions(user.getId(), PageRequest.of(0, limit))
                .stream().map(com.docint.dto.response.QueryHistoryDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages(UUID sessionId, int limit) {
        List<ChatMessage> messages = messageRepository.findLastNMessages(
                sessionId, PageRequest.of(0, limit));
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public ChatMessage saveMessage(UUID sessionId, ChatMessage.MessageRole role, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.chatSession(sessionId.toString()));
        ChatMessage msg = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .build();
        ChatMessage saved = messageRepository.save(msg);
        // bump session updatedAt
        sessionRepository.save(session);
        return saved;
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw ResourceNotFoundException.chatSession(sessionId.toString());
        }
        sessionRepository.deleteById(sessionId);
    }
}
