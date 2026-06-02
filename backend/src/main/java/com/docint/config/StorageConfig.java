package com.docint.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.docint.service.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${app.storage.default-provider:LOCAL}")
    private String defaultProvider;

    @Value("${app.storage.s3.region:us-east-1}")
    private String s3Region;

    @Value("${app.storage.s3.access-key:}")
    private String s3AccessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String s3SecretKey;

    @Value("${app.storage.azure.connection-string:}")
    private String azureConnectionString;

    /**
     * S3 client. Credentials resolution:
     *   - If app.storage.s3.access-key / secret-key are set (local dev) → use them directly.
     *   - Otherwise → fall back to the AWS default credential chain
     *     (env vars, ~/.aws/credentials, and — on EC2 — the instance IAM role).
     * This lets local dev keep keys in application-dev.yml while production on EC2
     * uses the instance role with NO keys anywhere.
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(s3Credentials())
                .build();
    }

    /** Presigner for generating short-lived direct-download URLs (offloads file bandwidth from the app). */
    @Bean
    public software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner() {
        return software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(s3Credentials())
                .build();
    }

    /**
     * Shared credential resolution for the S3 client and presigner:
     *   - static keys from config (local dev), or
     *   - the AWS default chain (env / profile / EC2 instance role).
     */
    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider s3Credentials() {
        if (StringUtils.hasText(s3AccessKey) && StringUtils.hasText(s3SecretKey)) {
            log.info("S3 using static credentials from configuration");
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(s3AccessKey, s3SecretKey));
        }
        log.info("S3 using AWS default credential chain (env / profile / IAM role)");
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public BlobServiceClient blobServiceClient() {
        if (azureConnectionString.isBlank()) {
            // Return a no-op client for local dev
            return new BlobServiceClientBuilder()
                    .connectionString("DefaultEndpointsProtocol=https;AccountName=devstoreaccount1;" +
                            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OGLjX+N+oarBQj6yYmtmrQ==;" +
                            "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;")
                    .buildClient();
        }
        return new BlobServiceClientBuilder()
                .connectionString(azureConnectionString)
                .buildClient();
    }

    @Bean
    @Primary
    public StorageService primaryStorageService(StorageResolver resolver) {
        log.info("Default storage provider: {}", defaultProvider);
        return resolver.resolve(defaultProvider);
    }

    /**
     * Resolves a StorageService by provider name (LOCAL, S3, AZURE_BLOB, SHAREPOINT).
     * Lets the upload endpoint route each document to a chosen backend.
     */
    @Bean
    public StorageResolver storageResolver(LocalStorageService local,
                                           S3StorageService s3,
                                           AzureBlobStorageService azure,
                                           SharePointStorageService sharePoint) {
        return provider -> switch (provider == null ? "LOCAL" : provider.toUpperCase()) {
            case "S3" -> s3;
            case "AZURE_BLOB" -> azure;
            case "SHAREPOINT" -> sharePoint;
            default -> local;
        };
    }

    @FunctionalInterface
    public interface StorageResolver {
        StorageService resolve(String provider);
    }
}
