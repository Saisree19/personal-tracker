package com.personal.tracker.auth.service;

import com.personal.tracker.auth.domain.ResetTokenEntity;
import com.personal.tracker.auth.domain.UserEntity;
import com.personal.tracker.auth.model.BasicResponse;
import com.personal.tracker.auth.model.RegisterRequest;
import com.personal.tracker.auth.model.ResetPasswordRequest;
import com.personal.tracker.auth.repository.ResetTokenRepository;
import com.personal.tracker.auth.repository.UserRepository;
import com.personal.tracker.common.security.JwtService;
import com.personal.tracker.common.security.JwtService.TokenResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthService {

    private final UserStore userStore;
    private final UserRepository userRepository;
    private final ResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DevMailSink mailSink;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration resetTtl;
    private final String frontendResetBaseUrl;
    private final boolean sinkEnabled;
    private final boolean mailAvailable;
    private final String mailFrom;
    private final ConcurrentMap<String, Integer> loginAttempts = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration OTP_REUSE_WINDOW = Duration.ofMinutes(2);

    public AuthService(UserStore userStore,
                       UserRepository userRepository,
                       ResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DevMailSink mailSink,
                       ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${security.reset.ttl-minutes:15}") long resetTtlMinutes,
                       @Value("${security.reset.frontend-url:http://localhost:5173/reset}") String frontendResetBaseUrl,
                       @Value("${mail.sink.enabled:false}") boolean sinkEnabled,
                       @Value("${mail.smtp.host:}") String smtpHost,
                       @Value("${mail.smtp.from:}") String smtpFrom) {
        this.userStore = userStore;
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSink = mailSink;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.resetTtl = Duration.ofMinutes(resetTtlMinutes);
        this.frontendResetBaseUrl = frontendResetBaseUrl;
        this.sinkEnabled = sinkEnabled;
        boolean hasHost = smtpHost != null && !smtpHost.isBlank();
        this.mailAvailable = hasHost && this.mailSender != null;
        this.mailFrom = smtpFrom != null && !smtpFrom.isBlank() ? smtpFrom : smtpHost;
    }

    public Mono<TokenResult> authenticate(String username, String password) {
        return userStore.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Username is wrong")))
                .flatMap(user -> {
                    int attempts = loginAttempts.getOrDefault(user.username(), 0);
                    if (attempts >= MAX_LOGIN_ATTEMPTS) {
                        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum login attempts reached. Please reset your password."));
                    }

                    if (!passwordEncoder.matches(password, user.passwordHash())) {
                        int updated = loginAttempts.merge(user.username(), 1, Integer::sum);
                        int remaining = MAX_LOGIN_ATTEMPTS - updated;
                        String message = updated >= MAX_LOGIN_ATTEMPTS
                                ? "Maximum login attempts reached. Please reset your password."
                                : String.format("Incorrect password. %d retries remaining.", remaining);
                        HttpStatus status = updated >= MAX_LOGIN_ATTEMPTS ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED;
                        return Mono.error(new ResponseStatusException(status, message));
                    }

                    loginAttempts.remove(user.username());
                    return jwtService.issueToken(user.username(), user.roles());
                });
    }

    public Mono<TokenResult> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.username())
                .flatMap(usernameExists -> {
                    if (usernameExists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already in use"));
                    }
                    return userRepository.existsByEmail(request.email());
                })
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use"));
                    }
                    UserEntity entity = new UserEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setNewEntity(true);
                    entity.setUsername(request.username());
                    entity.setEmail(request.email());
                    entity.setPasswordHash(passwordEncoder.encode(request.password()));
                    entity.setRoles("ROLE_USER");
                    entity.setCreatedAt(Instant.now());
                    return userRepository.save(entity);
                })
                .flatMap(saved -> jwtService.issueToken(saved.getUsername(), List.of(saved.getRoles())));
    }

    public Mono<BasicResponse> initiateReset(String email) {
        String genericMessage = "If an account exists for this email, you'll receive reset instructions.";
        return userRepository.findByEmail(email)
                .flatMap(user -> createResetArtifacts(user, email, genericMessage))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not available")));
    }

    public Mono<BasicResponse> resetPassword(ResetPasswordRequest request) {
        Mono<ResetTokenEntity> tokenMono = request.token() != null && !request.token().isBlank()
                ? resetTokenRepository.findByTokenAndUsedFalse(request.token())
                : resetTokenRepository.findByOtpAndUsedFalse(request.otp());

        return tokenMono
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset request")))
                .flatMap(reset -> {
                    if (reset.getExpiresAt() == null || reset.getExpiresAt().isBefore(Instant.now())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset request"));
                    }
                    return userRepository.findById(reset.getUserId())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset request")))
                            .flatMap(user -> {
                                if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.email())) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset request"));
                                }
                                user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                                return userRepository.save(user)
                                        .then(markAllResetsUsed(reset.getUserId()))
                                        .then(Mono.just(BasicResponse.ok("Password reset successful")));
                            });
                });
    }

    private Mono<BasicResponse> createResetArtifacts(UserEntity user, String email, String message) {
        boolean useSink = sinkEnabled;
        return findReusableReset(user.getId())
            .flatMap(existing -> sendResetPayload(email, message, useSink, existing))
            .switchIfEmpty(Mono.defer(() -> createNewReset(user, email, message, useSink)));
    }

    private Mono<BasicResponse> createNewReset(UserEntity user, String email, String message, boolean useSink) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(resetTtl);
        String token = UUID.randomUUID().toString();
        String otp = formatOtp();

        ResetTokenEntity entity = new ResetTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setNewEntity(true);
        entity.setUserId(user.getId());
        entity.setToken(token);
        entity.setOtp(otp);
        entity.setExpiresAt(expiresAt);
        entity.setUsed(false);
        entity.setCreatedAt(now);

        return resetTokenRepository.save(entity)
            .flatMap(saved -> sendResetPayload(email, message, useSink, saved));
    }

    private Mono<ResetTokenEntity> findReusableReset(UUID userId) {
        Instant now = Instant.now();
        Instant reuseThreshold = now.minus(OTP_REUSE_WINDOW);
        return resetTokenRepository.findByUserIdAndUsedFalse(userId)
                .filter(reset -> reset.getExpiresAt() != null && reset.getExpiresAt().isAfter(now))
                .filter(reset -> reset.getCreatedAt() != null && reset.getCreatedAt().isAfter(reuseThreshold))
                .sort(Comparator.comparing(ResetTokenEntity::getCreatedAt).reversed())
                .next();
    }

    private Mono<BasicResponse> sendResetPayload(String email, String message, boolean useSink, ResetTokenEntity reset) {
        String resetUrl = buildResetUrl(email, reset.getToken());
        return deliverReset(email, resetUrl, reset.getToken(), reset.getOtp(), reset.getExpiresAt(), useSink)
                .then(Mono.just(responseWithHints(message, useSink, reset.getToken(), reset.getOtp(), resetUrl)));
    }

    private Mono<Void> deliverReset(String email, String resetUrl, String token, String otp, Instant expiresAt, boolean useSink) {
        if (mailAvailable) {
            return Mono.fromRunnable(() -> sendMail(email, resetUrl, otp, expiresAt))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        }
        if (useSink) {
            mailSink.record(email, token, otp, expiresAt, resetUrl);
        } else {
            System.out.printf("Reset not delivered: SMTP not configured and sink disabled for %s%n", email);
        }
        return Mono.<Void>empty();
    }

    private BasicResponse responseWithHints(String message, boolean useSink, String token, String otp, String resetUrl) {
        if (!useSink) {
            return BasicResponse.ok(message);
        }
        return new BasicResponse(message, token, otp, resetUrl);
    }

    private Mono<Void> markAllResetsUsed(UUID userId) {
        return resetTokenRepository.findByUserIdAndUsedFalse(userId)
                .flatMap(reset -> {
                    reset.setUsed(true);
                    return resetTokenRepository.save(reset);
                })
                .then();
    }

    private String buildResetUrl(String email, String token) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return String.format("%s?token=%s&email=%s", frontendResetBaseUrl, token, encodedEmail);
    }

    private String formatOtp() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private void sendMail(String to, String resetUrl, String otp, Instant expiresAt) {
        if (mailSender == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(to);
        message.setSubject("Reset your Personal Tracker password");
        String body = "You requested a password reset.\n\n" +
                "Reset link: " + resetUrl + "\n" +
                "OTP: " + otp + "\n" +
                "Expires at: " + expiresAt.toString() + "\n\n" +
                "If you did not request this, you can ignore this email.";
        message.setText(body);
        mailSender.send(message);
    }
}
