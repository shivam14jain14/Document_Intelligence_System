package com.docint.service;

import com.docint.entity.AuditLog;
import com.docint.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String userEmail, String action, String target, String details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .userEmail(userEmail)
                    .action(action)
                    .target(target)
                    .details(details)
                    .build());
        } catch (Exception e) {
            // Auditing must never break the main flow
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
