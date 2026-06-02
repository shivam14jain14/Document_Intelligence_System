package com.docint.service;

import com.docint.dto.request.SyncRequest;
import com.docint.entity.Document;
import com.docint.entity.SyncJob;
import com.docint.entity.User;
import com.docint.exception.ResourceNotFoundException;
import com.docint.repository.SyncJobRepository;
import com.docint.repository.UserRepository;
import com.docint.service.storage.AzureBlobStorageService;
import com.docint.service.storage.LocalStorageService;
import com.docint.service.storage.SharePointStorageService;
import com.docint.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;
    private final UserRepository userRepository;
    private final IngestionService ingestionService;
    private final LocalStorageService localStorageService;
    private final AzureBlobStorageService azureBlobStorageService;
    private final SharePointStorageService sharePointStorageService;

    @Transactional
    public SyncJob createJob(SyncRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        Map<String, String> config = new HashMap<>();
        if (request.container() != null) config.put("container", request.container());
        if (request.prefix() != null) config.put("prefix", request.prefix());
        if (request.siteUrl() != null) config.put("siteUrl", request.siteUrl());
        if (request.libraryName() != null) config.put("libraryName", request.libraryName());
        if (request.localPath() != null) config.put("localPath", request.localPath());

        Document.StorageSource sourceType = Document.StorageSource.valueOf(request.source().toUpperCase());

        SyncJob job = SyncJob.builder()
                .sourceType(sourceType)
                .sourceConfig(config)
                .category(request.category())
                .status(SyncJob.SyncStatus.PENDING)
                .initiatedBy(user)
                .build();

        return syncJobRepository.save(job);
    }

    @Async("syncExecutor")
    public CompletableFuture<Void> runSync(UUID jobId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> ResourceNotFoundException.syncJob(jobId.toString()));

        job.setStatus(SyncJob.SyncStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        syncJobRepository.save(job);

        try {
            switch (job.getSourceType()) {
                case LOCAL -> syncLocal(job);
                case AZURE_BLOB -> syncAzureBlob(job);
                case SHAREPOINT -> syncSharePoint(job);
                default -> throw new UnsupportedOperationException("Sync not supported for: " + job.getSourceType());
            }
            job.setStatus(SyncJob.SyncStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Sync job {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(SyncJob.SyncStatus.FAILED);
            job.incrementFailed(e.getMessage());
        } finally {
            job.setCompletedAt(LocalDateTime.now());
            syncJobRepository.save(job);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void syncLocal(SyncJob job) {
        String path = job.getSourceConfig().getOrDefault("localPath", ".");
        var files = localStorageService.listFiles(path);
        job.setTotalFiles(files.size());
        syncJobRepository.save(job);

        for (StorageService.StoredFile file : files) {
            try {
                InputStream stream = localStorageService.download(file.key());
                ingestionService.ingestStream(stream, file.name(), job.getCategory(),
                        Document.StorageSource.LOCAL, job.getInitiatedBy());
                job.incrementProcessed();
            } catch (Exception e) {
                log.warn("Failed to sync file {}: {}", file.name(), e.getMessage());
                job.incrementFailed(file.name() + ": " + e.getMessage());
            }
            syncJobRepository.save(job);
        }
    }

    private void syncAzureBlob(SyncJob job) {
        String container = job.getSourceConfig().get("container");
        String prefix = job.getSourceConfig().getOrDefault("prefix", "");
        var files = azureBlobStorageService.listFilesInContainer(container, prefix);
        job.setTotalFiles(files.size());
        syncJobRepository.save(job);

        for (StorageService.StoredFile file : files) {
            try {
                InputStream stream = azureBlobStorageService.downloadFromContainer(container, file.key());
                ingestionService.ingestStream(stream, file.name(), job.getCategory(),
                        Document.StorageSource.AZURE_BLOB, job.getInitiatedBy());
                job.incrementProcessed();
            } catch (Exception e) {
                log.warn("Azure sync failed for {}: {}", file.name(), e.getMessage());
                job.incrementFailed(file.name() + ": " + e.getMessage());
            }
            syncJobRepository.save(job);
        }
    }

    private void syncSharePoint(SyncJob job) {
        String driveId = job.getSourceConfig().get("driveId");
        String folderId = job.getSourceConfig().get("folderId");
        var items = sharePointStorageService.listDriveItems(driveId, folderId);
        job.setTotalFiles(items.size());
        syncJobRepository.save(job);

        for (StorageService.StoredFile item : items) {
            try {
                InputStream stream = sharePointStorageService.downloadItem(driveId, item.key());
                ingestionService.ingestStream(stream, item.name(), job.getCategory(),
                        Document.StorageSource.SHAREPOINT, job.getInitiatedBy());
                job.incrementProcessed();
            } catch (Exception e) {
                log.warn("SharePoint sync failed for {}: {}", item.name(), e.getMessage());
                job.incrementFailed(item.name() + ": " + e.getMessage());
            }
            syncJobRepository.save(job);
        }
    }
}
