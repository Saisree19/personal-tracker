package com.personal.tracker.auth.service;

import com.personal.tracker.auth.domain.UserEntity;
import com.personal.tracker.auth.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Profile("!inmemory")
public class UserRepositoryUserStore implements UserStore {

    private final UserRepository userRepository;

    public UserRepositoryUserStore(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserRecord> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToRecord);
    }

    private UserRecord mapToRecord(UserEntity entity) {
        return new UserRecord(entity.getUsername(), entity.getPasswordHash(), parseRoles(entity.getRoles()));
    }

    private List<String> parseRoles(String roles) {
        List<String> parsed = Arrays.stream(roles == null ? new String[0] : roles.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
        return parsed.isEmpty() ? List.of("ROLE_USER") : parsed;
    }
}
