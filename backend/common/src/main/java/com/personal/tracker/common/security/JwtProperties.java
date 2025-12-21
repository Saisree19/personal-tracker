package com.personal.tracker.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration ttl) {
    public String secret() {
        return secret == null || secret.isBlank() ? "dev-secret-key-32-chars-long-123456" : secret;
    }

    public Duration ttl() {
        return ttl == null ? Duration.ofHours(1) : ttl;
    }
}
