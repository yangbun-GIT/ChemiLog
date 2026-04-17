package com.chemilog.main.api.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long premiumUsers,
        long suspendedUsers,
        long foodCount,
        long additiveCount,
        long todayMealLogs,
        long weeklyMealLogs,
        long policyViolationLogs,
        long hallucinationLogs,
        LocalDate generatedDate,
        List<CategoryStat> categoryStats,
        List<RecentFood> recentFoods
) {
    public record CategoryStat(
            String category,
            long itemCount
    ) {
    }

    public record RecentFood(
            Long foodId,
            String name,
            String category,
            String manufacturer,
            BigDecimal calories
    ) {
    }
}
