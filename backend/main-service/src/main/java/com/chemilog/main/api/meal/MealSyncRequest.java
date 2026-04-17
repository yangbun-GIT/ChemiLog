package com.chemilog.main.api.meal;

import com.chemilog.main.domain.meal.MealType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealSyncRequest(
        @NotNull(message = "meal_type은 필수입니다.")
        @JsonAlias("meal_type")
        MealType mealType,
        @NotNull(message = "logged_date는 필수입니다.")
        @JsonAlias("logged_date")
        LocalDate loggedDate,
        @NotEmpty(message = "items는 비어 있을 수 없습니다.")
        @Valid
        List<Item> items
) {
    public record Item(
            @NotNull(message = "food_id는 필수입니다.")
            @JsonAlias("food_id")
            Long foodId,
            @NotNull(message = "quantity는 필수입니다.")
            @DecimalMin(value = "0.01", message = "quantity는 0보다 커야 합니다.")
            BigDecimal quantity
    ) {
    }
}
