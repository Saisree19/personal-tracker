package com.personal.tracker.common.error;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standardized error response")
public record ApiError(
	@Schema(description = "HTTP status code", example = "400") int status,
	@Schema(description = "Application-specific error code", example = "GENERIC_ERROR") String code,
	@Schema(description = "Human readable error message", example = "Validation failed") String message,
	@Schema(description = "Request path", example = "/api/tasks") String path,
	@Schema(description = "Timestamp of the error in ISO-8601", example = "2024-01-01T12:00:00Z") Instant timestamp) {
}
