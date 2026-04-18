package com.chemilog.main.api.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAdditiveUpsertRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be <= 100 characters")
        String name,
        @Size(max = 50, message = "purpose must be <= 50 characters")
        String purpose,
        @Min(value = 1, message = "dangerLevel must be between 1 and 5")
        @Max(value = 5, message = "dangerLevel must be between 1 and 5")
        Integer dangerLevel,
        @Size(max = 100, message = "dailyAcceptableIntake must be <= 100 characters")
        String dailyAcceptableIntake
) {
}
