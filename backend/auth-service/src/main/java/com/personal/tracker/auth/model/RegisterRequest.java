package com.personal.tracker.auth.model;

import com.personal.tracker.common.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Please enter username") String username,
        @NotBlank(message = "Please enter email") @Email(message = "Please enter a valid email") String email,
        @NotBlank(message = "Please enter password") @ValidPassword String password
) {
}
