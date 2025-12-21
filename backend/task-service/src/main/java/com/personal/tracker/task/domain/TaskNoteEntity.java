package com.personal.tracker.task.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.domain.Persistable;

@Table("task_notes")
public class TaskNoteEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("task_id")
    private UUID taskId;

    @Column("user_id")
    private String userId;

    @Column("content")
    private String content;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = false;

    public TaskNoteEntity() {
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNewEntity(boolean isNew) {
        this.isNew = isNew;
    }
}
