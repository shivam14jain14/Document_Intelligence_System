package com.docint.controller;

import com.docint.dto.request.CreateUserRequest;
import com.docint.dto.request.SyncRequest;
import com.docint.dto.response.DashboardStats;
import com.docint.dto.response.SyncJobDTO;
import com.docint.dto.response.UserDTO;
import com.docint.entity.AuditLog;
import com.docint.entity.SyncJob;
import com.docint.exception.ResourceNotFoundException;
import com.docint.repository.AuditLogRepository;
import com.docint.repository.SyncJobRepository;
import com.docint.service.StatsService;
import com.docint.service.SyncJobService;
import com.docint.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SyncJobService syncJobService;
    private final SyncJobRepository syncJobRepository;
    private final UserManagementService userManagementService;
    private final StatsService statsService;
    private final AuditLogRepository auditLogRepository;

    // ───────────────────────── Dashboard ─────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> stats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    // ───────────────────────── User management ───────────────────
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest req,
                                              @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userManagementService.createUser(req, admin.getUsername()));
    }

    @PostMapping("/users/{id}/approve")
    public ResponseEntity<UserDTO> approve(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(userManagementService.approve(id, admin.getUsername()));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserDTO> setStatus(@PathVariable UUID id,
                                             @RequestBody Map<String, String> body,
                                             @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(userManagementService.setStatus(id, body.get("status"), admin.getUsername()));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> setRole(@PathVariable UUID id,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserDetails admin) {
        return ResponseEntity.ok(userManagementService.setRole(id, body.get("role"), admin.getUsername()));
    }

    // ───────────────────────── Audit log ─────────────────────────
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> audit(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }

    // ───────────────────────── Source sync ───────────────────────
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> triggerSync(@Valid @RequestBody SyncRequest request,
                                                           @AuthenticationPrincipal UserDetails admin) {
        SyncJob job = syncJobService.createJob(request, admin.getUsername());
        syncJobService.runSync(job.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("jobId", job.getId().toString(), "status", "RUNNING"));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<SyncJobDTO> getJob(@PathVariable UUID jobId) {
        return syncJobRepository.findById(jobId)
                .map(SyncJobDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ResourceNotFoundException.syncJob(jobId.toString()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<SyncJobDTO>> listJobs(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                syncJobRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                        .map(SyncJobDTO::from));
    }
}
