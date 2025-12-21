package com.personal.tracker.task.repository;

import java.util.UUID;

import com.personal.tracker.task.domain.TaskEntity;
import com.personal.tracker.task.domain.TaskStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TaskRepository extends ReactiveCrudRepository<TaskEntity, UUID> {

    Flux<TaskEntity> findByUserId(String userId);

    Flux<TaskEntity> findByUserIdAndStatusNot(String userId, TaskStatus status);

    Mono<TaskEntity> findByIdAndUserId(UUID id, String userId);
}
