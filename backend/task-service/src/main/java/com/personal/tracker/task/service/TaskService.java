package com.personal.tracker.task.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.personal.tracker.task.domain.TaskEntity;
import com.personal.tracker.task.domain.TaskNoteEntity;
import com.personal.tracker.task.domain.TaskStatus;
import com.personal.tracker.task.dto.TaskCreateRequest;
import com.personal.tracker.task.dto.TaskNoteRequest;
import com.personal.tracker.task.dto.TaskPageResponse;
import com.personal.tracker.task.dto.TaskResponse;
import com.personal.tracker.task.dto.TaskStatusUpdateRequest;
import com.personal.tracker.task.dto.TaskUpdateRequest;
import com.personal.tracker.task.repository.TaskNoteRepository;
import com.personal.tracker.task.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskNoteRepository taskNoteRepository;
    private final TaskMapper taskMapper;
    private final R2dbcEntityTemplate template;

        private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "due", "deadlineDate",
            "complexity", "complexity",
            "created", "createdAt"
        );

    public TaskService(TaskRepository taskRepository, TaskNoteRepository taskNoteRepository, TaskMapper taskMapper,
            R2dbcEntityTemplate template) {
        this.taskRepository = taskRepository;
        this.taskNoteRepository = taskNoteRepository;
        this.taskMapper = taskMapper;
        this.template = template;
    }

    public Mono<TaskResponse> createTask(String userId, TaskCreateRequest request) {
        Instant now = Instant.now();
        TaskEntity entity = new TaskEntity();
        entity.setId(UUID.randomUUID());
        entity.setNewEntity(true);
        entity.setUserId(userId);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setApplication(request.application());
        entity.setComplexity(request.complexity());
        entity.setDeadlineDate(request.deadlineDate());
        TaskStatus status = request.status() != null ? request.status() : TaskStatus.OPEN;
        entity.setStatus(status);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (status == TaskStatus.IN_PROGRESS) {
            entity.setStartedAt(now);
        }
        if (status == TaskStatus.CLOSED) {
            entity.setStartedAt(now);
            entity.setClosedAt(now);
            entity.setArchivedAt(now);
        }
        return taskRepository.save(entity).flatMap(this::buildResponse);
    }

    public Mono<TaskResponse> updateTask(String userId, UUID taskId, TaskUpdateRequest request) {
        return requireOwnedTask(userId, taskId)
                .flatMap(this::ensureNotArchived)
                .flatMap(task -> {
                    task.setTitle(request.title());
                    task.setDescription(request.description());
                    task.setApplication(request.application());
                    task.setComplexity(request.complexity());
                    task.setDeadlineDate(request.deadlineDate());
                    task.setUpdatedAt(Instant.now());
                    return taskRepository.save(task);
                })
                .flatMap(this::buildResponse);
    }

    public Mono<TaskResponse> appendNote(String userId, UUID taskId, TaskNoteRequest request) {
        return requireOwnedTask(userId, taskId)
                .flatMap(this::ensureNotArchived)
                .flatMap(task -> {
                    TaskNoteEntity note = new TaskNoteEntity();
                    note.setId(UUID.randomUUID());
                    note.setNewEntity(true);
                    note.setTaskId(task.getId());
                    note.setUserId(userId);
                    note.setContent(request.content());
                    note.setCreatedAt(Instant.now());
                    return taskNoteRepository.save(note).thenReturn(task);
                })
                .flatMap(this::buildResponse);
    }

    public Mono<TaskResponse> updateStatus(String userId, UUID taskId, TaskStatusUpdateRequest request) {
        TaskStatus desiredStatus = request.status();
        LocalDate startDate = request.startDate();
        LocalDate closeDate = request.closeDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return requireOwnedTask(userId, taskId)
                .flatMap(task -> {
                    if (task.getStatus() == TaskStatus.CLOSED) {
                        if (desiredStatus == TaskStatus.CLOSED) {
                            return Mono.just(task);
                        }
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is archived and cannot be reopened"));
                    }
                    Instant now = Instant.now();

                    if (desiredStatus == TaskStatus.IN_PROGRESS) {
                        if (startDate != null && startDate.isAfter(today)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be in the future"));
                        }
                        Instant startInstant = startDate != null
                                ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                                : now;
                        task.setStatus(TaskStatus.IN_PROGRESS);
                        task.setStartedAt(startInstant);
                        task.setUpdatedAt(now);
                        return taskRepository.save(task);
                    }

                    if (desiredStatus == TaskStatus.CLOSED) {
                        if (startDate != null && startDate.isAfter(today)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be in the future"));
                        }
                        if (closeDate != null && closeDate.isAfter(today)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Close date cannot be in the future"));
                        }
                        LocalDate existingStartDate = task.getStartedAt() != null
                                ? task.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate()
                                : null;
                        LocalDate effectiveStartDate = startDate != null ? startDate : existingStartDate;
                        if (effectiveStartDate == null) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is required before closing a task"));
                        }

                        LocalDate effectiveCloseDate = closeDate != null ? closeDate : LocalDate.now(ZoneOffset.UTC);
                        if (effectiveCloseDate.isAfter(today)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Close date cannot be in the future"));
                        }
                        if (effectiveCloseDate.isBefore(effectiveStartDate)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Close date must be on or after the start date"));
                        }

                        if (task.getStartedAt() == null) {
                            task.setStartedAt(effectiveStartDate.atStartOfDay(ZoneOffset.UTC).toInstant());
                        }
                        Instant closeInstant = effectiveCloseDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                        task.setStatus(TaskStatus.CLOSED);
                        task.setClosedAt(closeInstant);
                        task.setArchivedAt(closeInstant);
                        task.setUpdatedAt(now);
                        return taskRepository.save(task);
                    }

                    task.setStatus(desiredStatus);
                    task.setUpdatedAt(now);
                    return taskRepository.save(task);
                })
                .flatMap(this::buildResponse);
    }

    public Mono<TaskResponse> getTask(String userId, UUID taskId) {
        return requireOwnedTask(userId, taskId).flatMap(this::buildResponse);
    }

    public Mono<TaskPageResponse> listTasks(String userId, boolean includeArchived, int page, int size, String sortField, String sortDirection) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;

        String normalizedSortField = sortField == null ? "due" : sortField.toLowerCase();
        String property = ALLOWED_SORT_FIELDS.getOrDefault(normalizedSortField, "deadlineDate");
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, property);

        Criteria criteria = Criteria.where("userId").is(userId);
        if (includeArchived) {
            criteria = criteria.and("status").is(TaskStatus.CLOSED);
        } else {
            criteria = criteria.and("status").not(TaskStatus.CLOSED);
        }

        Query pageQuery = Query.query(criteria).sort(sort).limit(safeSize).offset(offset);

        Mono<Long> totalMono = template.count(Query.query(criteria), TaskEntity.class);
        Flux<TaskResponse> items = template.select(TaskEntity.class)
                .matching(pageQuery)
                .all()
                .flatMap(this::buildResponse);

        return Mono.zip(totalMono, items.collectList())
                .map(tuple -> {
                    long totalElements = tuple.getT1();
                    int totalPages = (int) Math.max(1, Math.ceil(totalElements / (double) safeSize));
                    return new TaskPageResponse(tuple.getT2(), safePage, safeSize, totalElements, totalPages, includeArchived);
                });
    }

    private Mono<TaskEntity> requireOwnedTask(String userId, UUID taskId) {
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found")))
                .flatMap(task -> {
                    if (!task.getUserId().equals(userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to user"));
                    }
                    return Mono.just(task);
                });
    }

    private Mono<TaskEntity> ensureNotArchived(TaskEntity task) {
        if (task.getStatus() == TaskStatus.CLOSED) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is archived and cannot be modified"));
        }
        return Mono.just(task);
    }

    private Mono<TaskResponse> buildResponse(TaskEntity entity) {
        return taskNoteRepository.findByTaskId(entity.getId())
                .sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(taskMapper::toNoteResponse)
                .collectList()
                .map(notes -> taskMapper.toResponse(entity, notes));
    }
}
