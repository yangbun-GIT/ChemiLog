package com.chemilog.main.api.auth;

public record TokenResponse(
        String accessToken,
        long expiresInSeconds
) {
}
