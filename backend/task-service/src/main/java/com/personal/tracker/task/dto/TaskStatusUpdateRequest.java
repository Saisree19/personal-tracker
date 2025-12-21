package com.personal.tracker.task.dto;

import java.time.LocalDate;

import com.personal.tracker.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
	@NotNull TaskStatus status,
	LocalDate startDate,
	LocalDate closeDate
) {
}
