package com.docint.service.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.docint.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AzureBlobStorageService implements StorageService {

    private final BlobServiceClient blobServiceClient;

    @Value("${app.storage.azure.default-container:documents}")
    private String defaultContainer;

    @Override
    public String upload(String key, InputStream stream, long size, String contentType) {
        try {
            BlobContainerClient container = getOrCreateContainer(defaultContainer);
            container.getBlobClient(key)
                    .upload(stream, size, true);
            return key;
        } catch (Exception e) {
            throw new StorageException("Azure Blob upload failed for key: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return getOrCreateContainer(defaultContainer)
                    .getBlobClient(key)
                    .openInputStream();
        } catch (Exception e) {
            throw new StorageException("Azure Blob download failed for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            getOrCreateContainer(defaultContainer).getBlobClient(key).deleteIfExists();
        } catch (Exception e) {
            log.warn("Azure Blob delete failed for key: {}", key);
        }
    }

    @Override
    public boolean exists(String key) {
        return getOrCreateContainer(defaultContainer).getBlobClient(key).exists();
    }

    @Override
    public List<StoredFile> listFiles(String prefix) {
        return getOrCreateContainer(prefix).listBlobs().stream()
                .map(b -> new StoredFile(
                        b.getName(),
                        b.getName().substring(b.getName().lastIndexOf('/') + 1),
                        b.getProperties().getContentLength(),
                        b.getProperties().getContentType()))
                .collect(Collectors.toList());
    }

    public List<StoredFile> listFilesInContainer(String containerName, String prefix) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        return container.listBlobsByHierarchy(prefix).stream()
                .filter(b -> !b.isPrefix())
                .map(BlobItem::getName)
                .map(name -> new StoredFile(name,
                        name.substring(name.lastIndexOf('/') + 1), 0, "application/octet-stream"))
                .collect(Collectors.toList());
    }

    public InputStream downloadFromContainer(String containerName, String blobName) {
        return blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .openInputStream();
    }

    private BlobContainerClient getOrCreateContainer(String name) {
        BlobContainerClient client = blobServiceClient.getBlobContainerClient(name);
        if (!client.exists()) client.create();
        return client;
    }
}
