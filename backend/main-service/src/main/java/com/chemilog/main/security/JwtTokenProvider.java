package com.chemilog.main.security;

import com.chemilog.main.config.properties.JwtProperties;
import com.chemilog.main.domain.user.User;
import com.chemilog.main.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = toSecretKey(jwtProperties.secret());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.accessExpMinutes(), ChronoUnit.MINUTES);
        return buildToken(user, now, exp, "access");
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.refreshExpDays(), ChronoUnit.DAYS);
        return buildToken(user, now, exp, "refresh");
    }

    public boolean isValidToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthUser getAuthUser(String token) {
        Claims claims = parseClaims(token);
        return new AuthUser(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                UserRole.valueOf(claims.get("role", String.class))
        );
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return "refresh".equals(claims.get("typ", String.class));
    }

    private String buildToken(User user, Instant issuedAt, Instant expiresAt, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("tier", user.getRole() == UserRole.PREMIUM ? "PREMIUM" : "USER");
        claims.put("typ", type);
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claims(claims)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private SecretKey toSecretKey(String value) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(value);
        } catch (Exception e) {
            keyBytes = value.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(normalizeKeyLength(keyBytes));
    }

    private byte[] normalizeKeyLength(byte[] original) {
        if (original.length >= 32) {
            return original;
        }
        byte[] resized = new byte[32];
        System.arraycopy(original, 0, resized, 0, original.length);
        for (int i = original.length; i < resized.length; i++) {
            resized[i] = (byte) (i * 31);
        }
        return resized;
    }
}
