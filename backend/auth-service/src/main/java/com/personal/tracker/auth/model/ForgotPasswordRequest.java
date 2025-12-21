package com.personal.tracker.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Please enter email") @Email(message = "Please enter a valid email") String email
) {
}
