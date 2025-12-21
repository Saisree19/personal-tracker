package com.personal.tracker.auth.model;

public record BasicResponse(String message, String resetToken, String otp, String resetUrl) {
    public static BasicResponse ok(String message) {
        return new BasicResponse(message, null, null, null);
    }

    public BasicResponse withHints(String token, String otp, String resetUrl) {
        return new BasicResponse(message, token, otp, resetUrl);
    }
}
