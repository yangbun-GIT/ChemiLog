package com.chemilog.main.api.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserRowResponse(
        Long userId,
        String email,
        String role,
        String status,
        String goal,
        String strictness,
        List<String> allergies,
        LocalDateTime createdAt
) {
}
