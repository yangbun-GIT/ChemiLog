package com.chemilog.main.api.meal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealHistoryResponse(
        List<DaySummary> days
) {
    public record DaySummary(
            LocalDate date,
            BigDecimal totalCalories,
            int itemCount
    ) {
    }
}
