package com.personal.tracker.auth.controller;

import com.personal.tracker.auth.model.AuthRequest;
import com.personal.tracker.auth.model.AuthResponse;
import com.personal.tracker.auth.model.BasicResponse;
import com.personal.tracker.auth.model.ForgotPasswordRequest;
import com.personal.tracker.auth.model.RegisterRequest;
import com.personal.tracker.auth.model.ResetPasswordRequest;
import com.personal.tracker.auth.service.AuthService;
import com.personal.tracker.common.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "User authentication and token issuance")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(
            summary = "Login",
            description = "Authenticate user credentials and issue a signed JWT",
            responses = {
                @ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request.username(), request.password())
                .map(token -> new AuthResponse(token.token(), token.expiresAt().toString()));
    }

        @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Register", description = "Create a new account with email and password",
            responses = {
                @ApiResponse(responseCode = "200", description = "Registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            })
        public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
            .map(token -> new AuthResponse(token.token(), token.expiresAt().toString()));
        }

        @PostMapping(path = "/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Forgot password", description = "Request a password reset link or OTP",
            responses = {
                @ApiResponse(responseCode = "200", description = "Request accepted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = BasicResponse.class))),
                @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            })
        public Mono<BasicResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.initiateReset(request.email());
        }

        @PostMapping(path = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
        @Operation(summary = "Reset password", description = "Reset password using token or OTP",
            responses = {
                @ApiResponse(responseCode = "200", description = "Password reset",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = BasicResponse.class))),
                @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            })
        public Mono<BasicResponse> reset(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.hasTokenOrOtp()) {
            return Mono.error(new IllegalArgumentException("token or otp is required"));
        }
        return authService.resetPassword(request);
        }
}
