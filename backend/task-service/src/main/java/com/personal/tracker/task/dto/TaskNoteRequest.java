package com.personal.tracker.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskNoteRequest(@NotBlank String content) {
}
