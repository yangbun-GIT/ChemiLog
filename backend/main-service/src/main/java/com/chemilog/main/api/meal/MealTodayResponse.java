package com.chemilog.main.api.meal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealTodayResponse(
        LocalDate loggedDate,
        List<Item> items
) {
    public record Item(
            Long foodId,
            BigDecimal quantity
    ) {
    }
}
