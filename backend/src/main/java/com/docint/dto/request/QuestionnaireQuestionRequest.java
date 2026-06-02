package com.docint.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionnaireQuestionRequest(
        @NotBlank String questionKey,
        @NotBlank String questionText,
        @NotNull Integer sortOrder,
        boolean active
) {}
