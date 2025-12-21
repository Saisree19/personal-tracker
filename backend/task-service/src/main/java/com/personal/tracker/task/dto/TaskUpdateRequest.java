package com.personal.tracker.task.dto;

import java.time.LocalDate;

import com.personal.tracker.task.domain.TaskComplexity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskUpdateRequest(
        @NotBlank String title,
        String description,
        @NotBlank String application,
        @NotNull TaskComplexity complexity,
        @NotNull LocalDate deadlineDate
) {
}
