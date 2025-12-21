package com.personal.tracker.task.controller;

import java.time.LocalDate;
import java.util.List;

import com.personal.tracker.common.security.JwtService;
import com.personal.tracker.task.domain.TaskComplexity;
import com.personal.tracker.task.domain.TaskStatus;
import com.personal.tracker.task.dto.TaskCreateRequest;
import com.personal.tracker.task.dto.TaskPageResponse;
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
                .bodyValue(new TaskStatusUpdateRequest(TaskStatus.CLOSED, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)))
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
        void closeFailsForFutureDatesOrInvalidOrdering() {
        TaskResponse task = createTaskAs("date-guard", "Guarded Task");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        webTestClient.post()
            .uri("/api/tasks/{id}/status", task.id())
            .header("Authorization", bearer("date-guard"))
            .bodyValue(new TaskStatusUpdateRequest(TaskStatus.CLOSED, tomorrow, tomorrow))
            .exchange()
            .expectStatus().isBadRequest();

        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate close = start.minusDays(1);
        webTestClient.post()
            .uri("/api/tasks/{id}/status", task.id())
            .header("Authorization", bearer("date-guard"))
            .bodyValue(new TaskStatusUpdateRequest(TaskStatus.CLOSED, start, close))
            .exchange()
            .expectStatus().isBadRequest();

        LocalDate sameDay = LocalDate.now().minusDays(2);
        webTestClient.post()
            .uri("/api/tasks/{id}/status", task.id())
            .header("Authorization", bearer("date-guard"))
            .bodyValue(new TaskStatusUpdateRequest(TaskStatus.CLOSED, sameDay, sameDay))
            .exchange()
            .expectStatus().isOk();
        }

    @Test
    void listReturnsOnlyOwnTasks() {
        TaskResponse ownerTask = createTaskAs("owner-2", "Owner Task 2");
        createTaskAs("other", "Other Task");

        TaskPageResponse page = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks").queryParam("includeArchived", false).build())
                .header("Authorization", bearer("owner-2"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskPageResponse.class)
                .returnResult()
                .getResponseBody();

        if (page == null) {
            throw new AssertionError("Task list did not return a body");
        }
        if (page.content().size() != 1) {
            throw new AssertionError("Expected exactly one task for owner");
        }
        TaskResponse only = page.content().get(0);
        if (!only.id().equals(ownerTask.id())) {
            throw new AssertionError("Expected only owner's task in list");
        }
        if (page.totalElements() != 1 || page.totalPages() != 1) {
            throw new AssertionError("Expected single-page metadata for one task");
        }
    }

    @Test
    void listSupportsPagingAndSorting() {
        String userId = "pager-user";
        createTaskAs(userId, "Soon", LocalDate.now().plusDays(1), TaskStatus.OPEN);
        createTaskAs(userId, "Later", LocalDate.now().plusDays(3), TaskStatus.OPEN);
        createTaskAs(userId, "Middle", LocalDate.now().plusDays(2), TaskStatus.OPEN);

        TaskPageResponse pageOne = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks")
                        .queryParam("includeArchived", false)
                        .queryParam("page", 1)
                        .queryParam("size", 2)
                        .queryParam("sortField", "due")
                        .queryParam("sortDirection", "asc")
                        .build())
                .header("Authorization", bearer(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskPageResponse.class)
                .returnResult()
                .getResponseBody();

        if (pageOne == null) {
            throw new AssertionError("Page one response was null");
        }
        if (pageOne.content().size() != 2) {
            throw new AssertionError("Expected two tasks on first page");
        }
        if (!"Soon".equals(pageOne.content().get(0).title()) || !"Middle".equals(pageOne.content().get(1).title())) {
            throw new AssertionError("Tasks not sorted by due date ascending on first page");
        }
        if (pageOne.totalElements() != 3 || pageOne.totalPages() != 2 || pageOne.page() != 1 || pageOne.size() != 2) {
            throw new AssertionError("Unexpected paging metadata on first page");
        }

        TaskPageResponse pageTwo = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks")
                        .queryParam("includeArchived", false)
                        .queryParam("page", 2)
                        .queryParam("size", 2)
                        .queryParam("sortField", "due")
                        .queryParam("sortDirection", "asc")
                        .build())
                .header("Authorization", bearer(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskPageResponse.class)
                .returnResult()
                .getResponseBody();

        if (pageTwo == null) {
            throw new AssertionError("Page two response was null");
        }
        if (pageTwo.content().size() != 1) {
            throw new AssertionError("Expected one task on second page");
        }
        if (!"Later".equals(pageTwo.content().get(0).title())) {
            throw new AssertionError("Unexpected task on second page");
        }
        if (pageTwo.includeArchived()) {
            throw new AssertionError("includeArchived should be false when not requested");
        }
    }

    @Test
    void archivedFilterReturnsOnlyClosedTasks() {
        String userId = "archive-view";
        TaskResponse openTask = createTaskAs(userId, "Active", LocalDate.now().plusDays(7), TaskStatus.OPEN);
        createTaskAs(userId, "Archived", LocalDate.now().plusDays(10), TaskStatus.CLOSED);

        TaskPageResponse archivedOnly = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks")
                        .queryParam("includeArchived", true)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .header("Authorization", bearer(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskPageResponse.class)
                .returnResult()
                .getResponseBody();

        if (archivedOnly == null) {
            throw new AssertionError("Archived filter response was null");
        }
        if (archivedOnly.content().size() != 1) {
            throw new AssertionError("Expected only archived tasks when filter enabled");
        }
        if (archivedOnly.content().get(0).status() != TaskStatus.CLOSED) {
            throw new AssertionError("Non-archived task returned in archived-only view");
        }

        TaskPageResponse activeOnly = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/tasks")
                        .queryParam("includeArchived", false)
                        .queryParam("page", 1)
                        .queryParam("size", 10)
                        .build())
                .header("Authorization", bearer(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskPageResponse.class)
                .returnResult()
                .getResponseBody();

        if (activeOnly == null) {
            throw new AssertionError("Active filter response was null");
        }
        if (activeOnly.content().stream().anyMatch(t -> t.status() == TaskStatus.CLOSED)) {
            throw new AssertionError("Archived tasks should be excluded when includeArchived is false");
        }
        if (activeOnly.content().stream().noneMatch(t -> t.id().equals(openTask.id()))) {
            throw new AssertionError("Expected open task to be present when includeArchived is false");
        }
    }

    private TaskResponse createTaskAs(String userId, String title) {
        TaskCreateRequest request = sampleCreateRequest(title);
        return createTaskAs(userId, request);
    }

    private TaskResponse createTaskAs(String userId, String title, LocalDate dueDate, TaskStatus status) {
        TaskCreateRequest request = sampleCreateRequest(title, dueDate, status);
        return createTaskAs(userId, request);
    }

    private TaskResponse createTaskAs(String userId, TaskCreateRequest request) {
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
        return sampleCreateRequest(title, LocalDate.now().plusDays(5), null);
    }

    private TaskCreateRequest sampleCreateRequest(String title, LocalDate deadline, TaskStatus status) {
        return new TaskCreateRequest(title, "desc", "app", TaskComplexity.MEDIUM, deadline, status);
    }

    private String bearer(String userId) {
        return jwtService.issueToken(userId, List.of("USER"))
                .map(result -> "Bearer " + result.token())
                .block();
    }
}
