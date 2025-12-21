package com.personal.tracker.auth.controller;

import com.personal.tracker.auth.domain.UserEntity;
import com.personal.tracker.auth.model.AuthRequest;
import com.personal.tracker.auth.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "demo", passwordEncoder.encode("password"), "ROLE_USER", Instant.now());
        user.setNewEntity(true);

        userRepository.deleteAll()
            .then(userRepository.save(user))
            .block();
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("demo", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.expiresAt").isNotEmpty();
    }

    @Test
    void loginWithInvalidCredentialsFails() {
        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("demo", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
