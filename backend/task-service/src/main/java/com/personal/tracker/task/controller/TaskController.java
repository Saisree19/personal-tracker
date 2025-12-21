package com.personal.tracker.task.controller;

import java.util.UUID;

import com.personal.tracker.common.error.ApiError;
import com.personal.tracker.task.dto.TaskCreateRequest;
import com.personal.tracker.task.dto.TaskNoteRequest;
import com.personal.tracker.task.dto.TaskResponse;
import com.personal.tracker.task.dto.TaskStatusUpdateRequest;
import com.personal.tracker.task.dto.TaskUpdateRequest;
import com.personal.tracker.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
@Validated
@Tag(name = "Tasks", description = "Task lifecycle management for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
        @Operation(
            summary = "Create task",
            description = "Create a task scoped to the authenticated user",
            responses = {
                @ApiResponse(responseCode = "201", description = "Task created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TaskResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<ResponseEntity<TaskResponse>> createTask(@Valid @RequestBody TaskCreateRequest request, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.createTask(userId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PutMapping("/{id}")
        @Operation(
            summary = "Update task",
            description = "Update mutable task fields for the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Task updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TaskResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<TaskResponse> updateTask(@PathVariable("id") UUID id, @Valid @RequestBody TaskUpdateRequest request, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.updateTask(userId, id, request);
    }

    @PostMapping("/{id}/notes")
        @Operation(
            summary = "Append note",
            description = "Append a note to a task while preserving existing notes",
            responses = {
                @ApiResponse(responseCode = "200", description = "Note appended",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TaskResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<TaskResponse> appendNote(@PathVariable("id") UUID id, @Valid @RequestBody TaskNoteRequest request, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.appendNote(userId, id, request);
    }

    @PostMapping("/{id}/status")
        @Operation(
            summary = "Update status",
            description = "Transition a task status, enforcing archival rules",
            responses = {
                @ApiResponse(responseCode = "200", description = "Status updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TaskResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid transition or request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<TaskResponse> updateStatus(@PathVariable("id") UUID id, @Valid @RequestBody TaskStatusUpdateRequest request, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.updateStatus(userId, id, request);
    }

    @GetMapping
        @Operation(
            summary = "List tasks",
            description = "List tasks for the authenticated user with optional archived inclusion",
            responses = {
                @ApiResponse(responseCode = "200", description = "Tasks listed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Flux<TaskResponse> listTasks(@RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.listTasks(userId, includeArchived);
    }

    @GetMapping("/{id}")
        @Operation(
            summary = "Get task",
            description = "Retrieve a single task scoped to the authenticated user",
            responses = {
                @ApiResponse(responseCode = "200", description = "Task found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = TaskResponse.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ApiError.class)))
            }
        )
    public Mono<TaskResponse> getTask(@PathVariable("id") UUID id, Authentication authentication) {
        String userId = authentication.getName();
        return taskService.getTask(userId, id);
    }
}
