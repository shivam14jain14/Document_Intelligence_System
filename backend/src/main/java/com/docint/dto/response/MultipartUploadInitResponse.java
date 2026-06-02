package com.docint.dto.response;

import java.util.List;

public record MultipartUploadInitResponse(
        String uploadId,
        String key,
        long partSizeBytes,
        List<PartUrl> parts
) {
    public record PartUrl(int partNumber, String url) {}
}
