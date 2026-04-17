package com.chemilog.main.api.food;

import java.math.BigDecimal;
import java.util.List;

public record FoodResponse(
        Long foodId,
        String name,
        String category,
        String manufacturer,
        String imageUrl,
        BigDecimal calories,
        BigDecimal carbs,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal sugars,
        BigDecimal sodium,
        List<Long> additiveIds,
        List<String> warningLabels
) {
}
