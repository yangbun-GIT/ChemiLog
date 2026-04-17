package com.chemilog.main.api.common;

public record PageInfo(
        int currentPage,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
}
