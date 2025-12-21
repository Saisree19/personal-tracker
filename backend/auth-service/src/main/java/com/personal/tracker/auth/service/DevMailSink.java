package com.personal.tracker.auth.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class DevMailSink {

    public record ResetHint(String email, String token, String otp, Instant expiresAt, String resetUrl) {}

    private final AtomicReference<ResetHint> last = new AtomicReference<>();

    public void record(String email, String token, String otp, Instant expiresAt, String resetUrl) {
        last.set(new ResetHint(email, token, otp, expiresAt, resetUrl));
    }

    public ResetHint last() {
        return last.get();
    }
}
