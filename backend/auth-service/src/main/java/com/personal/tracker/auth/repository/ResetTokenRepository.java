package com.personal.tracker.auth.repository;

import com.personal.tracker.auth.domain.ResetTokenEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResetTokenRepository extends ReactiveCrudRepository<ResetTokenEntity, UUID> {
    Mono<ResetTokenEntity> findByTokenAndUsedFalse(String token);

    Mono<ResetTokenEntity> findByOtpAndUsedFalse(String otp);

    Flux<ResetTokenEntity> findByUserIdAndUsedFalse(UUID userId);
}
