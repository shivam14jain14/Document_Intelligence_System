package com.docint.service;

import com.docint.config.CacheConfig;
import com.docint.dto.response.ChatResponse;
import com.docint.dto.response.SourceChunkDTO;
import com.docint.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagAgentService {

    private final ChatClient chatClient;
    private final ChatSessionService chatSessionService;
    private final com.docint.repository.DocumentRepository documentRepository;
    private final CacheManager cacheManager;

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private static final Pattern SOURCE_PATTERN =
            Pattern.compile("\\[Source:\\s*([^,\\]]+),?([^\\]]*)\\]");

    @SuppressWarnings("deprecation")
    public ChatResponse chat(UUID sessionId, String userMessage, String categoryFilter) {
        chatSessionService.saveMessage(sessionId, ChatMessage.MessageRole.USER, userMessage);

        // Answer cache: identical (question + category) returns instantly, skipping Claude.
        // Evicted whenever documents change; 10-min TTL backstop.
        String cacheKey = answerCacheKey(userMessage, categoryFilter);
        Cache answerCache = cacheManager.getCache(CacheConfig.CHAT_ANSWERS);
        if (answerCache != null) {
            CachedAnswer hit = answerCache.get(cacheKey, CachedAnswer.class);
            if (hit != null) {
                log.info("[chatAnswers] cache HIT — skipping LLM for: {}", userMessage);
                ChatMessage cachedMsg = chatSessionService.saveMessage(
                        sessionId, ChatMessage.MessageRole.ASSISTANT, hit.getAnswer());
                return new ChatResponse(cachedMsg.getId(), sessionId, hit.getAnswer(), hit.getSources());
            }
        }

        List<ChatMessage> history = chatSessionService.getRecentMessages(sessionId, 10);
        List<Message> springMessages = buildMessageHistory(history, userMessage);

        String systemPrompt = buildSystemPrompt(categoryFilter);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .messages(springMessages)
                .functions("vectorSearch", "getDocumentMetadata", "listCategories")
                .call()
                .content();

        List<SourceChunkDTO> sources = extractSources(answer);
        if (answerCache != null) {
            answerCache.put(cacheKey, new CachedAnswer(answer, sources));
        }
        ChatMessage saved = chatSessionService.saveMessage(
                sessionId, ChatMessage.MessageRole.ASSISTANT, answer);

        return new ChatResponse(saved.getId(), sessionId, answer, sources);
    }

    private String answerCacheKey(String question, String categoryFilter) {
        String cat = (categoryFilter == null || categoryFilter.isBlank()) ? "all" : categoryFilter.toLowerCase();
        return question.trim().toLowerCase() + "|" + cat;
    }

    /** Cached LLM answer + its citations. Plain class (not record) for reliable JSON (Redis) serialization. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CachedAnswer {
        private String answer;
        private List<SourceChunkDTO> sources;
    }

    @SuppressWarnings("deprecation")
    public Flux<String> chatStream(UUID sessionId, String userMessage, String categoryFilter) {
        chatSessionService.saveMessage(sessionId, ChatMessage.MessageRole.USER, userMessage);

        List<ChatMessage> history = chatSessionService.getRecentMessages(sessionId, 10);
        List<Message> springMessages = buildMessageHistory(history, userMessage);
        String systemPrompt = buildSystemPrompt(categoryFilter);

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .system(systemPrompt)
                .messages(springMessages)
                .functions("vectorSearch", "getDocumentMetadata", "listCategories")
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    try {
                        chatSessionService.saveMessage(
                                sessionId, ChatMessage.MessageRole.ASSISTANT, fullResponse.toString());
                    } catch (Exception e) {
                        log.error("Failed to persist streamed response for session {}", sessionId, e);
                    }
                });
    }

    private List<Message> buildMessageHistory(List<ChatMessage> history, String currentMessage) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            if (msg.getRole() == ChatMessage.MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(currentMessage));
        return messages;
    }

    private String buildSystemPrompt(String categoryFilter) {
        String base;
        try {
            base = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            base = "You are a document assistant. Answer only from retrieved context. Always cite sources.";
        }
        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("all")) {
            base += "\n\nIMPORTANT: The user has selected category filter: '" + categoryFilter +
                    "'. When calling vectorSearch, always pass category='" + categoryFilter + "'.";
        }
        return base;
    }

    private List<SourceChunkDTO> extractSources(String answer) {
        java.util.LinkedHashMap<String, SourceChunkDTO> unique = new java.util.LinkedHashMap<>();
        Matcher matcher = SOURCE_PATTERN.matcher(answer);
        while (matcher.find()) {
            String docName = matcher.group(1).trim();
            String section = matcher.group(2).trim();
            // Resolve the cited filename to a real document ID so the UI can build a download link
            String documentId = documentRepository.findFirstByNameOrderByCreatedAtDesc(docName)
                    .map(d -> d.getId().toString())
                    .orElse("");
            unique.putIfAbsent(docName, new SourceChunkDTO(documentId, docName, "", section));
        }
        return new ArrayList<>(unique.values());
    }
}
