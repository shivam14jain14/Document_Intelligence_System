package com.docint.service;

import com.docint.dto.response.CategoryDTO;
import com.docint.dto.response.DocumentDTO;
import com.docint.entity.Document;
import com.docint.exception.ResourceNotFoundException;
import com.docint.config.StorageConfig;
import com.docint.repository.DocumentRepository;
import com.docint.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageConfig.StorageResolver storageResolver;

    @Transactional(readOnly = true)
    public Page<DocumentDTO> list(String category, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Document> docs;
        if (category != null && status != null) {
            docs = documentRepository.findByCategoryAndStatus(
                    category, Document.DocumentStatus.valueOf(status), pageable);
        } else if (category != null) {
            docs = documentRepository.findByCategory(category, pageable);
        } else if (status != null) {
            docs = documentRepository.findByStatus(Document.DocumentStatus.valueOf(status), pageable);
        } else {
            docs = documentRepository.findAll(pageable);
        }
        return docs.map(DocumentDTO::from);
    }

    @Transactional(readOnly = true)
    public DocumentDTO getById(UUID id) {
        return documentRepository.findById(id)
                .map(DocumentDTO::from)
                .orElseThrow(() -> ResourceNotFoundException.document(id.toString()));
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategories() {
        return documentRepository.countByCategory().stream()
                .map(row -> new CategoryDTO((String) row[0], (Long) row[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public InputStream downloadFile(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.document(id.toString()));
        // Route to the backend the document was actually stored in (LOCAL/S3/AZURE_BLOB),
        // not the default provider.
        StorageService storage = storageResolver.resolve(doc.getSource().name());
        return storage.download(doc.getSourcePath());
    }

    /**
     * Returns a short-lived direct-download URL when the backend supports it (S3 pre-signed URL),
     * letting the browser download straight from storage and offloading bandwidth from the app.
     * Empty for backends without it (e.g. local FS) → caller streams instead.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<String> getPresignedDownloadUrl(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.document(id.toString()));
        return storageResolver.resolve(doc.getSource().name())
                .presignedDownloadUrl(doc.getSourcePath(), java.time.Duration.ofMinutes(10));
    }
}
