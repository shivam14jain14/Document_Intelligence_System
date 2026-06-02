package com.docint.controller;

import com.docint.dto.request.QuestionnaireQuestionRequest;
import com.docint.dto.response.QuestionnaireQuestionDTO;
import com.docint.service.QuestionnaireQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/questionnaire-questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionnaireQuestionController {

    private final QuestionnaireQuestionService questionnaireQuestionService;

    @GetMapping
    public ResponseEntity<List<QuestionnaireQuestionDTO>> list() {
        return ResponseEntity.ok(questionnaireQuestionService.listAll());
    }

    @PostMapping
    public ResponseEntity<QuestionnaireQuestionDTO> create(@Valid @RequestBody QuestionnaireQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnaireQuestionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionnaireQuestionDTO> update(@PathVariable UUID id,
                                                           @Valid @RequestBody QuestionnaireQuestionRequest request) {
        return ResponseEntity.ok(questionnaireQuestionService.update(id, request));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<QuestionnaireQuestionDTO> setActive(@PathVariable UUID id,
                                                              @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(questionnaireQuestionService.setActive(id, Boolean.TRUE.equals(body.get("active"))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        questionnaireQuestionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
