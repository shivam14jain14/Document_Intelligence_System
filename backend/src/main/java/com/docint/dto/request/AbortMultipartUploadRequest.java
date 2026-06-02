package com.docint.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AbortMultipartUploadRequest(
        @NotBlank String uploadId,
        @NotBlank String key
) {}
