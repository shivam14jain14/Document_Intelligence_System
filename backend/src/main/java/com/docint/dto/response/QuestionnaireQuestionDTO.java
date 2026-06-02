package com.docint.dto.response;

import com.docint.entity.QuestionnaireQuestion;

import java.util.UUID;

public record QuestionnaireQuestionDTO(
        UUID id,
        String questionKey,
        String questionText,
        boolean active,
        int sortOrder
) {
    public static QuestionnaireQuestionDTO from(QuestionnaireQuestion q) {
        return new QuestionnaireQuestionDTO(
                q.getId(),
                q.getQuestionKey(),
                q.getQuestionText(),
                q.isActive(),
                q.getSortOrder()
        );
    }
}
