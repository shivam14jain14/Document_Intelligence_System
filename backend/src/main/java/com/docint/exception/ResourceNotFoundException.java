package com.docint.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException document(String id) {
        return new ResourceNotFoundException("Document not found: " + id);
    }

    public static ResourceNotFoundException chatSession(String id) {
        return new ResourceNotFoundException("Chat session not found: " + id);
    }

    public static ResourceNotFoundException syncJob(String id) {
        return new ResourceNotFoundException("Sync job not found: " + id);
    }
}
