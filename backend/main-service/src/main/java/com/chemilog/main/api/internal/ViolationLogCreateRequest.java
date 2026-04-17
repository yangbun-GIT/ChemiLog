package com.chemilog.main.api.internal;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ViolationLogCreateRequest(
        Long userId,
        @NotBlank
        String inputText,
        String violationCategory,
        BigDecimal confidenceScore
) {
}
