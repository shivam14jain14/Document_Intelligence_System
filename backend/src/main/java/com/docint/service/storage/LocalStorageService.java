package com.docint.service.storage;

import com.docint.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${app.storage.local.base-path:./uploads}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath();
        try {
            Files.createDirectories(this.basePath);
            log.info("Local storage initialized at: {}", this.basePath);
        } catch (IOException e) {
            throw new StorageException("Could not create local storage directory", e);
        }
    }

    @Override
    public String upload(String key, InputStream stream, long size, String contentType) {
        Path target = basePath.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file locally: {}", target);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        Path file = basePath.resolve(key);
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new StorageException("File not found: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(basePath.resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", key);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(basePath.resolve(key));
    }

    @Override
    public List<StoredFile> listFiles(String prefix) {
        Path dir = basePath.resolve(prefix);
        if (!Files.exists(dir)) return List.of();
        try {
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        String key = basePath.relativize(p).toString().replace("\\", "/");
                        try {
                            return new StoredFile(key, p.getFileName().toString(),
                                    Files.size(p), "application/octet-stream");
                        } catch (IOException e) {
                            return new StoredFile(key, p.getFileName().toString(), 0, "application/octet-stream");
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to list files in: " + prefix, e);
        }
    }
}
