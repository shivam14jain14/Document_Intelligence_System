package com.docint.service;

import com.docint.dto.request.QuestionnaireQuestionRequest;
import com.docint.dto.response.QuestionnaireQuestionDTO;
import com.docint.entity.QuestionnaireQuestion;
import com.docint.exception.ConflictException;
import com.docint.exception.ResourceNotFoundException;
import com.docint.repository.QuestionnaireQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionnaireQuestionService {

    private final QuestionnaireQuestionRepository repository;

    @Transactional(readOnly = true)
    public List<QuestionnaireQuestionDTO> listAll() {
        return repository.findAllByOrderBySortOrderAscCreatedAtAsc()
                .stream().map(QuestionnaireQuestionDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireQuestionDTO> listActive() {
        return repository.findByActiveTrueOrderBySortOrderAscCreatedAtAsc()
                .stream().map(QuestionnaireQuestionDTO::from).toList();
    }

    @Transactional
    public QuestionnaireQuestionDTO create(QuestionnaireQuestionRequest request) {
        String key = normalizeKey(request.questionKey());
        if (repository.existsByQuestionKey(key)) {
            throw new ConflictException("Question key already exists: " + key);
        }
        QuestionnaireQuestion saved = repository.save(QuestionnaireQuestion.builder()
                .questionKey(key)
                .questionText(request.questionText().trim())
                .sortOrder(request.sortOrder())
                .active(request.active())
                .build());
        return QuestionnaireQuestionDTO.from(saved);
    }

    @Transactional
    public QuestionnaireQuestionDTO update(UUID id, QuestionnaireQuestionRequest request) {
        QuestionnaireQuestion question = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        String key = normalizeKey(request.questionKey());
        if (!question.getQuestionKey().equals(key) && repository.existsByQuestionKey(key)) {
            throw new ConflictException("Question key already exists: " + key);
        }
        question.setQuestionKey(key);
        question.setQuestionText(request.questionText().trim());
        question.setSortOrder(request.sortOrder());
        question.setActive(request.active());
        return QuestionnaireQuestionDTO.from(repository.save(question));
    }

    @Transactional
    public QuestionnaireQuestionDTO setActive(UUID id, boolean active) {
        QuestionnaireQuestion question = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
        question.setActive(active);
        return QuestionnaireQuestionDTO.from(repository.save(question));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Question not found: " + id);
        }
        repository.deleteById(id);
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}
