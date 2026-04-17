package com.chemilog.main.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chemilog.cors")
public record CorsProperties(String allowedOrigins) {
}
