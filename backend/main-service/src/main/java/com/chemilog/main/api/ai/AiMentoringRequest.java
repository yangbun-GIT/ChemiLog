package com.chemilog.main.api.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record AiMentoringRequest(
        @Valid
        @NotEmpty(message = "chat_history??理쒖냼 1媛??댁긽?댁뼱???⑸땲??")
        @JsonAlias("chat_history")
        List<ChatMessage> chatHistory,
        @Valid
        @JsonAlias("current_cart")
        List<CartItem> currentCart,
        @JsonAlias("profile_context")
        ProfileContext profileContext,
        @JsonAlias("meal_history")
        List<MealHistoryItem> mealHistory,
        @JsonAlias("selected_meal_type")
        String selectedMealType
) {
    public record ChatMessage(
            @NotNull
            String role,
            @NotNull
            String content
    ) {
    }

    public record CartItem(
            @JsonAlias("food_id")
            Long foodId,
            String name,
            BigDecimal quantity,
            @JsonAlias("additive_ids")
            List<Long> additiveIds
    ) {
    }

    public record ProfileContext(
            String goal,
            List<String> allergies,
            String strictness
    ) {
    }

    public record MealHistoryItem(
            String date,
            @JsonAlias("total_calories")
            BigDecimal totalCalories,
            @JsonAlias("item_count")
            Integer itemCount,
            @JsonAlias("top_additives")
            List<String> topAdditives
    ) {
    }
}
