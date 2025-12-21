package com.personal.tracker.auth.model;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Please enter username") String username,
        @NotBlank(message = "Please enter password") String password
) {
}
