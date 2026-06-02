package com.docint.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompleteMultipartUploadRequest(
        @NotBlank String uploadId,
        @NotBlank String key,
        @NotBlank String filename,
        @NotBlank String category,
        @NotBlank String contentType,
        @NotNull @Min(1) Long fileSizeBytes,
        @NotEmpty List<UploadedPart> parts
) {
    public record UploadedPart(
            @Min(1) int partNumber,
            @NotBlank String eTag
    ) {}
}
