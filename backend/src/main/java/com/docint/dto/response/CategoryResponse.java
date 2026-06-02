package com.docint.dto.response;

import com.docint.entity.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        long documentCount
) {
    public static CategoryResponse from(Category c, long docCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), docCount);
    }
}
