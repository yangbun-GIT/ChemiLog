package com.chemilog.main.api.meal;

import com.chemilog.main.domain.meal.MealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealDayResponse(
        LocalDate loggedDate,
        List<MealEntry> meals
) {
    public record MealEntry(
            Long mealId,
            MealType mealType,
            BigDecimal totalCalories,
            List<Item> items
    ) {
    }

    public record Item(
            Long detailId,
            Long foodId,
            String foodName,
            String category,
            BigDecimal quantity,
            BigDecimal calories
    ) {
    }
}
