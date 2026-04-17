package com.chemilog.main.api.auth;

import java.util.List;

public record UserMeResponse(
        Long userId,
        String email,
        String role,
        String status,
        String goal,
        String strictness,
        List<String> allergies
) {
}
