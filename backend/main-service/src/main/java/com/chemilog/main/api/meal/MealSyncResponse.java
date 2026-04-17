package com.chemilog.main.api.meal;

import java.math.BigDecimal;

public record MealSyncResponse(
        Long mealId,
        BigDecimal totalCalories,
        BigDecimal totalCarbs,
        BigDecimal totalProtein,
        BigDecimal totalFat
) {
}
