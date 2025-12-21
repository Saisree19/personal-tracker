package com.personal.tracker.task.service;

import java.util.List;

import com.personal.tracker.task.domain.TaskEntity;
import com.personal.tracker.task.domain.TaskNoteEntity;
import com.personal.tracker.task.dto.TaskNoteResponse;
import com.personal.tracker.task.dto.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(TaskEntity entity, List<TaskNoteResponse> notes) {
        return new TaskResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getApplication(),
                entity.getComplexity(),
                entity.getDeadlineDate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStartedAt(),
                entity.getClosedAt(),
                entity.getArchivedAt(),
                notes
        );
    }

    public TaskNoteResponse toNoteResponse(TaskNoteEntity entity) {
        return new TaskNoteResponse(entity.getId(), entity.getContent(), entity.getUserId(), entity.getCreatedAt());
    }
}
