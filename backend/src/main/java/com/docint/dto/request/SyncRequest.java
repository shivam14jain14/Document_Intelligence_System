package com.docint.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SyncRequest(
        @NotBlank String source,
        @NotBlank String category,
        String container,
        String prefix,
        String siteUrl,
        String libraryName,
        String localPath
) {}
