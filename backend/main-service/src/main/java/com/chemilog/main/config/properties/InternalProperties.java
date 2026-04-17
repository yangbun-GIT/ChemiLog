package com.chemilog.main.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chemilog.internal")
public record InternalProperties(
        String secret,
        String fastapiBaseUrl
) {
}
