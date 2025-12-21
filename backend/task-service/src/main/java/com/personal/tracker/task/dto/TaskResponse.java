package com.personal.tracker.task.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.personal.tracker.task.domain.TaskComplexity;
import com.personal.tracker.task.domain.TaskStatus;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        String application,
        TaskComplexity complexity,
        LocalDate deadlineDate,
        TaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant closedAt,
        Instant archivedAt,
        List<TaskNoteResponse> notes
) {
}
