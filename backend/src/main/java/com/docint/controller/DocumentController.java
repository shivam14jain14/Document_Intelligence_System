package com.docint.controller;

import com.docint.dto.request.AbortMultipartUploadRequest;
import com.docint.dto.request.CompleteMultipartUploadRequest;
import com.docint.dto.request.InitiateMultipartUploadRequest;
import com.docint.dto.response.MultipartUploadInitResponse;
import com.docint.dto.response.CategoryDTO;
import com.docint.dto.response.DocumentDTO;
import com.docint.entity.Document;
import com.docint.entity.User;
import com.docint.repository.UserRepository;
import com.docint.service.DocumentService;
import com.docint.service.IngestionService;
import com.docint.service.storage.S3StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentService documentService;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam(value = "storageProvider", defaultValue = "LOCAL") String storageProvider,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        Document doc = ingestionService.upload(file, category, storageProvider, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DocumentDTO.from(doc));
    }

    @PostMapping("/multipart/initiate")
    public ResponseEntity<MultipartUploadInitResponse> initiateMultipartUpload(
            @Valid @RequestBody InitiateMultipartUploadRequest request) {
        String documentKey = "documents/" + request.category() + "/" + UUID.randomUUID() + "/" + request.filename();
        String uploadId = s3StorageService.createMultipartUpload(documentKey, request.contentType());
        long partSize = S3StorageService.MULTIPART_PART_SIZE_BYTES;
        int totalParts = (int) Math.ceil((double) request.fileSizeBytes() / partSize);

        List<MultipartUploadInitResponse.PartUrl> parts = IntStream.rangeClosed(1, totalParts)
                .mapToObj(partNumber -> new MultipartUploadInitResponse.PartUrl(
                        partNumber,
                        s3StorageService.presignMultipartPart(documentKey, uploadId, partNumber, Duration.ofHours(1))
                ))
                .toList();

        return ResponseEntity.ok(new MultipartUploadInitResponse(uploadId, documentKey, partSize, parts));
    }

    @PostMapping("/multipart/complete")
    public ResponseEntity<DocumentDTO> completeMultipartUpload(
            @Valid @RequestBody CompleteMultipartUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        s3StorageService.completeMultipartUpload(
                request.key(),
                request.uploadId(),
                request.parts().stream()
                        .map(p -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                                .partNumber(p.partNumber())
                                .eTag(p.eTag())
                                .build())
                        .toList());
        Document doc = ingestionService.finalizeMultipartUpload(
                request.filename(),
                request.category(),
                request.contentType(),
                request.fileSizeBytes(),
                request.key(),
                user
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(DocumentDTO.from(doc));
    }

    @PostMapping("/multipart/abort")
    public ResponseEntity<Void> abortMultipartUpload(@Valid @RequestBody AbortMultipartUploadRequest request) {
        s3StorageService.abortMultipartUpload(request.key(), request.uploadId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.list(category, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingestionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> categories() {
        return ResponseEntity.ok(documentService.getCategories());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        DocumentDTO doc = documentService.getById(id);
        InputStreamResource resource = new InputStreamResource(documentService.downloadFile(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.name() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Returns a pre-signed direct-download URL for backends that support it (S3).
     * 200 {url} → download directly from storage; 204 → no URL, use the /download stream.
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<java.util.Map<String, String>> downloadUrl(@PathVariable UUID id) {
        return documentService.getPresignedDownloadUrl(id)
                .map(url -> ResponseEntity.ok(java.util.Map.of("url", url)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
