package com.docint.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardStats(
        long totalDocuments,
        long totalChunks,
        long totalUsers,
        long pendingUsers,
        long totalCategories,
        Map<String, Long> documentsByCategory,
        Map<String, Long> documentsByStatus,
        List<RecentActivity> recentActivity
) {
    public record RecentActivity(String userEmail, String action, String target, String createdAt) {}
}
