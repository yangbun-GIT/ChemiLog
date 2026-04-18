package com.chemilog.main.api.admin;

import java.util.List;

public record AdminUserUpdateRequest(
        String role,
        String status,
        String goal,
        String strictness,
        List<String> allergies
) {
}
