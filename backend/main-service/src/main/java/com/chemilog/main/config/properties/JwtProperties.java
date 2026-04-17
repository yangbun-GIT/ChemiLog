package com.chemilog.main.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chemilog.security.jwt")
public record JwtProperties(
        String secret,
        int accessExpMinutes,
        int refreshExpDays
) {
}
