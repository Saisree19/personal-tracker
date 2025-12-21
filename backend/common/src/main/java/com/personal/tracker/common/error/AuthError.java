package com.personal.tracker.common.error;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication error response with retry information")
public record AuthError(
    @Schema(description = "HTTP status code", example = "401") int status,
    @Schema(description = "Application-specific error code", example = "UNAUTHORIZED") String code,
    @Schema(description = "Human readable error message", example = "Incorrect password") String message,
    @Schema(description = "Request path", example = "/api/auth/login") String path,
    @Schema(description = "Timestamp of the error in ISO-8601", example = "2024-01-01T12:00:00Z") Instant timestamp,
    @Schema(description = "Number of retry attempts remaining", example = "3") Integer retriesRemaining
) {
    public static AuthError fromApiError(ApiError apiError, Integer retriesRemaining) {
        return new AuthError(
            apiError.status(),
            apiError.code(),
            apiError.message(),
            apiError.path(),
            apiError.timestamp(),
            retriesRemaining
        );
    }
}

