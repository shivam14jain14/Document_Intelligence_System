package com.docint.controller;

import com.docint.dto.request.ChatRequest;
import com.docint.dto.response.ChatMessageDTO;
import com.docint.dto.response.ChatResponse;
import com.docint.dto.response.ChatSessionDTO;
import com.docint.service.ChatSessionService;
import com.docint.service.RagAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagAgentService ragAgentService;
    private final ChatSessionService chatSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionDTO> createSession(
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String title = body != null ? body.get("title") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatSessionService.createSession(userDetails.getUsername(), title));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionDTO>> listSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatSessionService.listSessions(userDetails.getUsername()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<com.docint.dto.response.QueryHistoryDTO>> queryHistory(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatSessionService.getQueryHistory(userDetails.getUsername(), limit));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getHistory(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatSessionService.getHistory(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId) {
        chatSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatResponse> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(
                ragAgentService.chat(sessionId, request.message(), request.categoryFilter()));
    }

    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(
            @PathVariable UUID sessionId,
            @RequestParam String message,
            @RequestParam(required = false) String category) {
        return ragAgentService.chatStream(sessionId, message, category);
    }
}
