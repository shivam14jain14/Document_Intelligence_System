package com.docint.service.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface StorageService {

    String upload(String key, InputStream stream, long size, String contentType);

    InputStream download(String key);

    void delete(String key);

    boolean exists(String key);

    List<StoredFile> listFiles(String prefix);

    /**
     * Returns a short-lived, direct-download URL for the object, if the backend supports it
     * (e.g. an S3 pre-signed URL). Backends without this capability (local FS) return empty,
     * and callers fall back to streaming the bytes through the API.
     */
    default Optional<String> presignedDownloadUrl(String key, Duration ttl) {
        return Optional.empty();
    }

    record StoredFile(String key, String name, long size, String contentType) {}
}
