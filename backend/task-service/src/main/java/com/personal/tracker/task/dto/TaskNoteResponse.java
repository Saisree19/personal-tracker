package com.personal.tracker.task.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskNoteResponse(UUID id, String content, String authorId, Instant createdAt) {
}
