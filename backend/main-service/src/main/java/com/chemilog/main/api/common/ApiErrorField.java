package com.chemilog.main.api.common;

public record ApiErrorField(
        String field,
        Object value,
        String reason
) {
}
