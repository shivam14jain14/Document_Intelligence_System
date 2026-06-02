package com.docint.repository;

import com.docint.entity.QuestionnaireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionnaireQuestionRepository extends JpaRepository<QuestionnaireQuestion, UUID> {
    boolean existsByQuestionKey(String questionKey);
    List<QuestionnaireQuestion> findAllByOrderBySortOrderAscCreatedAtAsc();
    List<QuestionnaireQuestion> findByActiveTrueOrderBySortOrderAscCreatedAtAsc();
}
