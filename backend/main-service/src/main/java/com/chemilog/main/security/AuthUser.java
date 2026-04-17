package com.chemilog.main.security;

import com.chemilog.main.domain.user.UserRole;

public record AuthUser(
        Long userId,
        String email,
        UserRole role
) {
}
