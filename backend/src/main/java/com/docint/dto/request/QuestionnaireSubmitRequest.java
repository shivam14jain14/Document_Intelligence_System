package com.docint.dto.request;

import java.util.Map;

public record QuestionnaireSubmitRequest(
        Map<String, String> answers,
        Map<String, String> answerSources
) {}
