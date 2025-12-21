package com.personal.tracker.auth.config;

import com.personal.tracker.auth.domain.UserEntity;
import com.personal.tracker.auth.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

@Configuration
public class UserDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    @Bean
    public ApplicationRunner demoUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.init-demo-user:false}") boolean demoUserEnabled) {
        return args -> {
            if (!demoUserEnabled) {
                log.info("Demo user initialization skipped (auth.init-demo-user=false)");
                return;
            }

            userRepository.findByUsername("demo")
                    .switchIfEmpty(insertDemoUser(userRepository, passwordEncoder))
                    .doOnError(error -> log.error("Failed to initialize demo user", error))
                    .subscribe();
        };
    }

    private Mono<UserEntity> insertDemoUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        UserEntity user = new UserEntity(
            UUID.randomUUID(),
            "demo",
            passwordEncoder.encode("password"),
            "ROLE_USER",
            Instant.now());
        user.setNewEntity(true);
        return userRepository.save(user);
    }
}
