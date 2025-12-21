package com.personal.tracker.reporting.repository;

import java.time.Instant;
import java.util.UUID;

import com.personal.tracker.reporting.domain.TaskRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TaskRecordRepository extends ReactiveCrudRepository<TaskRecord, UUID> {
}
