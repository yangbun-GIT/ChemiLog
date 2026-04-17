package com.chemilog.main.api.common;

import java.time.OffsetDateTime;

public record ApiMeta(
        OffsetDateTime timestamp,
        Integer rateLimitRemaining
) {
}
