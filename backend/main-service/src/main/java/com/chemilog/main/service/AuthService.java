package com.chemilog.main.service;

import com.chemilog.main.api.auth.LoginRequest;
import com.chemilog.main.api.auth.RegisterRequest;
import com.chemilog.main.api.auth.TokenResponse;
import com.chemilog.main.config.properties.JwtProperties;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import com.chemilog.main.domain.user.UserStatus;
import com.chemilog.main.exception.ApiException;
import com.chemilog.main.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.chemilog.main.security.JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final TokenHashService tokenHashService;
    private final JwtProperties jwtProperties;
    private final LoginRateLimitService loginRateLimitService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            com.chemilog.main.security.JwtTokenProvider jwtTokenProvider,
            StringRedisTemplate redisTemplate,
            TokenHashService tokenHashService,
            JwtProperties jwtProperties,
            LoginRateLimitService loginRateLimitService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.tokenHashService = tokenHashService;
        this.jwtProperties = jwtProperties;
        this.loginRateLimitService = loginRateLimitService;
    }

    public AuthTokens register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTH-4090", "이미 가입된 이메일입니다.");
        }

        Map<String, Object> healthProfile = new HashMap<>();
        healthProfile.put("goal", isBlank(request.goal()) ? "MAINTAIN" : request.goal().trim());
        healthProfile.put("strictness", isBlank(request.strictness()) ? "MEDIUM" : request.strictness().trim());
        healthProfile.put("allergies", sanitizeAllergies(request.allergies()));

        User user = User.create(
                email,
                passwordEncoder.encode(request.password()),
                UserRole.USER,
                UserStatus.ACTIVE,
                healthProfile
        );
        User saved = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(saved);
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved);
        storeRefreshTokenHash(saved.getUserId(), refreshToken);

        return new AuthTokens(
                new TokenResponse(accessToken, jwtProperties.accessExpMinutes() * 60L),
                refreshToken
        );
    }

    public AuthTokens login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        if (loginRateLimitService.isLocked(clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AUTH-4290", "로그인 시도가 너무 많아 일시적으로 차단되었습니다.");
        }

        User user = userRepository.findByEmailAndDeletedFalse(normalizeEmail(request.username()))
                .orElseThrow(() -> invalidCredential(clientIp));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredential(clientIp);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTH-4030", "정지 또는 탈퇴 상태 계정입니다.");
        }

        loginRateLimitService.clearFailures(clientIp);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        storeRefreshTokenHash(user.getUserId(), refreshToken);

        return new AuthTokens(
                new TokenResponse(accessToken, jwtProperties.accessExpMinutes() * 60L),
                refreshToken
        );
    }

    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4001", "유효하지 않은 Refresh Token입니다.");
        }
        if (!jwtTokenProvider.isValidToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4001", "유효하지 않은 Refresh Token입니다.");
        }
        var authUser = jwtTokenProvider.getAuthUser(refreshToken);
        String storedHash = redisTemplate.opsForValue().get(refreshKey(authUser.userId()));
        String incomingHash = tokenHashService.sha256(refreshToken);
        if (storedHash == null || !storedHash.equals(incomingHash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4002", "Refresh Token이 만료되었거나 무효화되었습니다.");
        }

        User user = userRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4003", "사용자를 찾을 수 없습니다."));
        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4004", "인증할 수 없는 사용자입니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        return new TokenResponse(newAccessToken, jwtProperties.accessExpMinutes() * 60L);
    }

    public void logout(Long userId) {
        if (userId != null) {
            redisTemplate.delete(refreshKey(userId));
        }
    }

    public long refreshTtlSeconds() {
        return Duration.ofDays(jwtProperties.refreshExpDays()).toSeconds();
    }

    private ApiException invalidCredential(String clientIp) {
        loginRateLimitService.registerFailure(clientIp);
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH-4010", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private void storeRefreshTokenHash(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                refreshKey(userId),
                tokenHashService.sha256(refreshToken),
                Duration.ofDays(jwtProperties.refreshExpDays())
        );
    }

    private String refreshKey(Long userId) {
        return "RT:" + userId;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private List<String> sanitizeAllergies(List<String> allergies) {
        if (allergies == null) {
            return List.of();
        }
        return allergies.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AuthTokens(TokenResponse tokenResponse, String refreshToken) {
    }
}
