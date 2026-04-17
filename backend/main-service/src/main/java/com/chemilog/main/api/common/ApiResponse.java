package com.chemilog.main.api.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<ApiErrorField> errors,
        ApiMeta meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                true,
                "COMMON-2000",
                "OK",
                data,
                List.of(),
                new ApiMeta(OffsetDateTime.now(), null)
        );
    }

    public static <T> ApiResponse<T> fail(String code, String message, List<ApiErrorField> errors) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errors,
                new ApiMeta(OffsetDateTime.now(), null)
        );
    }
}
