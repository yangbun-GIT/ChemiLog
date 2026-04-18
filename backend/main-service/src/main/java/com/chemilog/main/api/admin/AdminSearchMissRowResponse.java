package com.chemilog.main.api.admin;

import java.time.LocalDateTime;

public record AdminSearchMissRowResponse(
        Long missId,
        String keyword,
        Integer hitCount,
        boolean resolved,
        LocalDateTime lastSeenAt
) {
}
