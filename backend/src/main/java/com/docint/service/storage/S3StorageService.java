package com.docint.service.storage;

import com.docint.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class S3StorageService implements StorageService {

    public static final long MULTIPART_PART_SIZE_BYTES = 64L * 1024L * 1024L;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3StorageService(S3Client s3Client,
                            S3Presigner s3Presigner,
                            @Value("${app.storage.s3.bucket:}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /**
     * Generates a time-limited pre-signed GET URL. The browser downloads directly from S3,
     * so file bytes never pass through our app server. Content-Disposition=attachment makes
     * the browser save (rather than display) the file.
     */
    @Override
    public Optional<String> presignedDownloadUrl(String key, Duration ttl) {
        try {
            String filename = key.substring(key.lastIndexOf('/') + 1);
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .responseContentDisposition("attachment; filename=\"" + filename + "\"")
                    .build();
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(getReq)
                    .build();
            return Optional.of(s3Presigner.presignGetObject(presignReq).url().toString());
        } catch (Exception e) {
            log.warn("Failed to presign URL for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public String createMultipartUpload(String key, String contentType) {
        try {
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                    CreateMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build());
            return response.uploadId();
        } catch (Exception e) {
            throw new StorageException("Could not create multipart upload for key: " + key, e);
        }
    }

    public String presignMultipartPart(String key, String uploadId, int partNumber, Duration ttl) {
        try {
            UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();
            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(ttl)
                    .uploadPartRequest(partRequest)
                    .build();
            return s3Presigner.presignUploadPart(presignRequest).url().toString();
        } catch (Exception e) {
            throw new StorageException("Could not presign multipart part " + partNumber + " for key: " + key, e);
        }
    }

    public void completeMultipartUpload(String key, String uploadId, List<CompletedPart> parts) {
        try {
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Could not complete multipart upload for key: " + key, e);
        }
    }

    public void abortMultipartUpload(String key, String uploadId) {
        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
        } catch (Exception e) {
            log.warn("Could not abort multipart upload {} for key {}: {}", uploadId, key, e.getMessage());
        }
    }

    @Override
    public String upload(String key, InputStream stream, long size, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromInputStream(stream, size));
            return key;
        } catch (Exception e) {
            throw new StorageException("S3 upload failed for key: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new StorageException("S3 download failed for key: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("S3 delete failed for key: {}", key);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public List<StoredFile> listFiles(String prefix) {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                    ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build());
            return response.contents().stream()
                    .map(o -> new StoredFile(o.key(),
                            o.key().substring(o.key().lastIndexOf('/') + 1),
                            o.size(), "application/octet-stream"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("Failed to list S3 objects with prefix: " + prefix, e);
        }
    }
}
