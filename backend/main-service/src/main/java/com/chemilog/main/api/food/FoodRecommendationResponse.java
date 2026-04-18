package com.chemilog.main.api.food;

import java.util.List;

public record FoodRecommendationResponse(
        String goal,
        String strictness,
        List<MealRecommendation> meals
) {
    public record MealRecommendation(
            String mealType,
            String reason,
            FoodResponse food
    ) {
    }
}
