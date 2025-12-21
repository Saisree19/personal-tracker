package com.personal.tracker.auth.model;

import com.personal.tracker.common.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Please enter email") @Email(message = "Please enter a valid email") String email,
        String token,
        String otp,
        @NotBlank(message = "Please enter password") @ValidPassword String newPassword
) {
    public boolean hasTokenOrOtp() {
        return (token != null && !token.isBlank()) || (otp != null && !otp.isBlank());
    }
}
