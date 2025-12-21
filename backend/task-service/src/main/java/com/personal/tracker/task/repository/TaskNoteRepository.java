package com.personal.tracker.task.repository;

import java.util.UUID;

import com.personal.tracker.task.domain.TaskNoteEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TaskNoteRepository extends ReactiveCrudRepository<TaskNoteEntity, UUID> {

    Flux<TaskNoteEntity> findByTaskId(UUID taskId);
}
