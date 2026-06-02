package com.docint.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String message,
        String categoryFilter
) {}
