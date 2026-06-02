package com.docint.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitiateMultipartUploadRequest(
        @NotBlank String filename,
        @NotBlank String category,
        @NotBlank String contentType,
        @NotNull @Min(1) Long fileSizeBytes
) {}
