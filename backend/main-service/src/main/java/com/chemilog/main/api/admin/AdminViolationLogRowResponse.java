package com.chemilog.main.api.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminViolationLogRowResponse(
        Long logId,
        LocalDateTime createdAt,
        Long userId,
        String userEmailMasked,
        String violationCategory,
        BigDecimal confidenceScore,
        String inputPreview
) {
}
