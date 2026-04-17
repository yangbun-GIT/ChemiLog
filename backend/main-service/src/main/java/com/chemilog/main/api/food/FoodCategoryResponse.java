package com.chemilog.main.api.food;

public record FoodCategoryResponse(
        String category,
        long itemCount
) {
}
