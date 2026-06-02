package com.docint.service;

import com.docint.config.CacheConfig;
import com.docint.dto.response.DashboardStats;
import com.docint.entity.User;
import com.docint.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(CacheConfig.STATS)
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        Map<String, Long> byCategory = documentRepository.countByCategory().stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> byStatus = documentRepository.findAll().stream()
                .collect(Collectors.groupingBy(d -> d.getStatus().name(), Collectors.counting()));

        List<DashboardStats.RecentActivity> recent = auditLogRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream()
                .map(a -> new DashboardStats.RecentActivity(
                        a.getUserEmail(), a.getAction(), a.getTarget(),
                        a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""))
                .toList();

        return new DashboardStats(
                documentRepository.count(),
                documentChunkRepository.count(),
                userRepository.count(),
                userRepository.countByStatus(User.Status.PENDING),
                categoryRepository.count(),
                byCategory,
                byStatus,
                recent
        );
    }
}
