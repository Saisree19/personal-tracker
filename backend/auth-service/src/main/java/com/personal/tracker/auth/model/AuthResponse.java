package com.personal.tracker.auth.model;

public record AuthResponse(String token, String expiresAt) {
}
