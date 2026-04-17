package com.chemilog.main.api.auth;

import com.chemilog.main.api.common.ApiResponse;
import com.chemilog.main.security.AuthUser;
import com.chemilog.main.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthTokens tokens = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken(), authService.refreshTtlSeconds()));
        return ResponseEntity.ok(ApiResponse.ok(tokens.tokenResponse()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        AuthService.AuthTokens tokens = authService.login(request, httpRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken(), authService.refreshTtlSeconds()));
        return ResponseEntity.ok(ApiResponse.ok(tokens.tokenResponse()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        TokenResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletResponse response
    ) {
        authService.logout(authUser == null ? null : authUser.userId());
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshCookie());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String buildRefreshCookie(String refreshToken, long ttlSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(ttlSeconds)
                .build()
                .toString();
    }

    private String expireRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }
}
