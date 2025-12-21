package com.personal.tracker.task.controller;

import java.time.LocalDate;
import java.util.List;

import com.personal.tracker.common.security.JwtService;
import com.personal.tracker.task.domain.TaskComplexity;
import com.personal.tracker.task.domain.TaskStatus;
import com.personal.tracker.task.dto.TaskCreateRequest;
import com.personal.tracker.task.dto.TaskResponse;
import com.personal.tracker.task.dto.TaskStatusUpdateRequest;
import com.personal.tracker.task.dto.TaskUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @Test
    void createAndGetTask() {
        String token = bearer("alice");
        TaskCreateRequest request = sampleCreateRequest("Task A");

        TaskResponse created = webTestClient.post()
                .uri("/api/tasks")
                .header("Authorization", token)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TaskResponse.class)
                .returnResult()
                .getResponseBody();

        if (created == null) {
            throw new AssertionError("Task creation did not return a body");
        }

        webTestClient.get()
                .uri("/api/tasks/{id}", created.id())
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Task A")
                .jsonPath("$.status").isEqualTo("OPEN");
    }

    @Test
    void updatingAnotherUsersTaskIsForbidden() {
        TaskResponse ownerTask = createTaskAs("owner", "Owner Task");
        TaskUpdateRequest update = new TaskUpdateRequest("Updated", "changed", "AppX", TaskComplexity.HIGH, LocalDate.now().plusDays(10));

        webTestClient.put()
                .uri("/api/tasks/{id}", ownerTask.id())
                .header("Authorization", bearer("intruder"))
                .bodyValue(update)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void appendNoteAndArchivePreventsFurtherEdits() {
        TaskResponse task = createTaskAs("note-user", "Note Task");

        webTestClient.post()
                .uri("/api/tasks/{id}/notes", task.id())
                .header("Authorization", bearer("note-user"))
                .bodyValue(new com.personal.tracker.task.dto.TaskNoteRequest("First note"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.notes.length()").isEqualTo(1);

        webTestClient.post()
                .uri("/api/tasks/{id}/status", task.id())
                .header("Authorization", bearer("note-user"))
                .bodyValue(new TaskStatusUpdateRequest(TaskStatus.CLOSED))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.archivedAt").isNotEmpty();

        webTestClient.post()
                .uri("/api/tasks/{id}/notes", task.id())
                .header("Authorization", bearer("note-user"))
                .bodyValue(new com.personal.tracker.task.dto.TaskNoteRequest("Another note"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void listReturnsOnlyOwnTasks() {
        TaskResponse ownerTask = createTaskAs("owner-2", "Owner Task 2");
        createTaskAs("other", "Other Task");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks").queryParam("includeArchived", false).build())
                .header("Authorization", bearer("owner-2"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TaskResponse.class)
                .hasSize(1)
                .value(tasks -> {
                    TaskResponse only = tasks.getFirst();
                    if (!only.id().equals(ownerTask.id())) {
                        throw new AssertionError("Expected only owner's task in list");
                    }
                });
    }

    private TaskResponse createTaskAs(String userId, String title) {
        TaskCreateRequest request = sampleCreateRequest(title);
        TaskResponse response = webTestClient.post()
                .uri("/api/tasks")
                .header("Authorization", bearer(userId))
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TaskResponse.class)
                .returnResult()
                .getResponseBody();
        if (response == null) {
            throw new AssertionError("Task creation did not return a body");
        }
        return response;
    }

    private TaskCreateRequest sampleCreateRequest(String title) {
        return new TaskCreateRequest(title, "desc", "app", TaskComplexity.MEDIUM, LocalDate.now().plusDays(5), null);
    }

    private String bearer(String userId) {
        return jwtService.issueToken(userId, List.of("USER"))
                .map(result -> "Bearer " + result.token())
                .block();
    }
}
