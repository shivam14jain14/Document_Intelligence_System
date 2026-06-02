package com.docint.service.storage;

import com.docint.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@Slf4j
public class SharePointStorageService implements StorageService {

    @Value("${app.storage.sharepoint.tenant-id:}")
    private String tenantId;

    @Value("${app.storage.sharepoint.client-id:}")
    private String clientId;

    @Value("${app.storage.sharepoint.client-secret:}")
    private String clientSecret;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String upload(String key, InputStream stream, long size, String contentType) {
        throw new UnsupportedOperationException("SharePoint upload not implemented for local keys");
    }

    @Override
    public InputStream download(String key) {
        throw new StorageException("Use downloadFromSharePoint(siteUrl, driveId, itemId) instead");
    }

    @Override
    public void delete(String key) {
        throw new UnsupportedOperationException("SharePoint delete not implemented");
    }

    @Override
    public boolean exists(String key) {
        return false;
    }

    @Override
    public List<StoredFile> listFiles(String prefix) {
        return List.of();
    }

    public String getAccessToken() {
        if (tenantId.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            throw new StorageException("SharePoint credentials not configured");
        }
        try {
            String body = "grant_type=client_credentials" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=https://graph.microsoft.com/.default";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            int start = responseBody.indexOf("\"access_token\":\"") + 16;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        } catch (Exception e) {
            throw new StorageException("Failed to get SharePoint access token", e);
        }
    }

    public List<StoredFile> listDriveItems(String driveId, String folderId) {
        String token = getAccessToken();
        String url = "https://graph.microsoft.com/v1.0/drives/" + driveId +
                "/items/" + (folderId != null ? folderId : "root") + "/children";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("SharePoint list response: {}", response.statusCode());
            return List.of();
        } catch (Exception e) {
            throw new StorageException("Failed to list SharePoint drive items", e);
        }
    }

    public InputStream downloadItem(String driveId, String itemId) {
        String token = getAccessToken();
        String url = "https://graph.microsoft.com/v1.0/drives/" + driveId + "/items/" + itemId + "/content";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return response.body();
        } catch (Exception e) {
            throw new StorageException("Failed to download SharePoint item: " + itemId, e);
        }
    }
}
