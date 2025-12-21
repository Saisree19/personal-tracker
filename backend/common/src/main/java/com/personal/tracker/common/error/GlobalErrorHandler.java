package com.personal.tracker.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@org.springframework.web.bind.annotation.RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiError>> handleResponseStatusException(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = switch (status) {
            case UNAUTHORIZED -> ErrorCodes.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCodes.FORBIDDEN;
            case NOT_FOUND -> ErrorCodes.NOT_FOUND;
            default -> ErrorCodes.GENERIC;
        };
        ApiError body = buildError(status, code, ex.getReason(), exchange);
        return Mono.just(ResponseEntity.status(status).body(body));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiError>> handleValidationException(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    // Use the custom message if available, otherwise use field name with default message
                    String errorMessage = error.getDefaultMessage();
                    if (errorMessage != null && !errorMessage.isBlank()) {
                        return errorMessage;
                    }
                    // Fallback: format field name nicely
                    String fieldName = error.getField();
                    return "Please enter " + fieldName;
                })
                .findFirst()
                .orElse("Validation failed");
        ApiError body = buildError(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION, message, exchange);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ApiError>> handleThrowable(Throwable ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiError body = buildError(status, ErrorCodes.GENERIC, ex.getMessage(), exchange);
        return Mono.just(ResponseEntity.status(status).body(body));
    }

    private ApiError buildError(HttpStatus status, String code, String message, ServerWebExchange exchange) {
        String safeMessage = message == null || message.isBlank() ? status.getReasonPhrase() : message;
        String path = exchange != null && exchange.getRequest() != null ? exchange.getRequest().getPath().value() : "";
        return new ApiError(status.value(), code, safeMessage, path, Instant.now());
    }
}
