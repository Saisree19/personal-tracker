package com.personal.tracker.auth.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
@Profile("inmemory")
public class InMemoryUserStore implements UserStore {

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public InMemoryUserStore(PasswordEncoder passwordEncoder) {
        // No default users; populate externally or via tests.
    }

    @Override
    public Mono<UserRecord> findByUsername(String username) {
        return Mono.justOrEmpty(users.get(username));
    }
}
