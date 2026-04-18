package com.chemilog.main.api.admin;

import java.time.LocalDateTime;

public record AdminHallucinationLogRowResponse(
        Long logId,
        LocalDateTime createdAt,
        String modelVersion,
        String failedReason,
        String promptPreview,
        String responsePreview
) {
}
