package com.personal.tracker.task.dto;

import java.time.LocalDate;

import com.personal.tracker.task.domain.TaskComplexity;
import com.personal.tracker.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskCreateRequest(
        @NotBlank String title,
        String description,
        @NotBlank String application,
        @NotNull TaskComplexity complexity,
        @NotNull LocalDate deadlineDate,
        TaskStatus status
) {
}
