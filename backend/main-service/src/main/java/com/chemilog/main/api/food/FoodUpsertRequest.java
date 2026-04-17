package com.chemilog.main.api.food;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record FoodUpsertRequest(
        @NotBlank(message = "식품명은 필수입니다.")
        @Size(max = 150)
        String name,
        @Size(max = 50)
        String category,
        @Size(max = 100)
        String manufacturer,
        @Size(max = 50)
        String barcode,
        @Size(max = 512)
        String imageUrl,
        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal calories,
        @DecimalMin(value = "0.00")
        BigDecimal carbs,
        @DecimalMin(value = "0.00")
        BigDecimal protein,
        @DecimalMin(value = "0.00")
        BigDecimal fat,
        @DecimalMin(value = "0.00")
        BigDecimal sugars,
        @DecimalMin(value = "0.00")
        BigDecimal sodium,
        List<Long> additiveIds
) {
}
