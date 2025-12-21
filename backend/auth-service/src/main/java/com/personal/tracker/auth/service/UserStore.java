package com.personal.tracker.auth.service;

import reactor.core.publisher.Mono;

public interface UserStore {
    Mono<UserRecord> findByUsername(String username);
}
