package com.docint.dto.response;

import java.util.List;
import java.util.Map;

/** The question set + the user's current answers (empty if not started). */
public record QuestionnaireDTO(
        List<QuestionnaireQuestionDTO> questions,
        Map<String, String> answers,
        Map<String, String> answerSources,
        boolean completed
) {}
