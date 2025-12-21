package com.personal.tracker.reporting.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.personal.tracker.common.security.JwtService;
import com.personal.tracker.reporting.domain.TaskComplexity;
import com.personal.tracker.reporting.domain.TaskRecord;
import com.personal.tracker.reporting.domain.TaskStatus;
import com.personal.tracker.reporting.repository.TaskRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class ReportingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TaskRecordRepository repository;

    private String aliceBearer;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
        Instant now = Instant.now();

        insert("alice", "app1", TaskComplexity.MEDIUM, now.minus(2, ChronoUnit.DAYS));
        insert("alice", "app1", TaskComplexity.HIGH, now.minus(10, ChronoUnit.DAYS));
        insert("alice", "app2", TaskComplexity.MEDIUM, now.minus(5, ChronoUnit.DAYS));
        insert("alice", "app2", TaskComplexity.LOW, now.minus(50, ChronoUnit.DAYS)); // outside monthly window
        insert("bob", "app1", TaskComplexity.LOW, now.minus(1, ChronoUnit.DAYS)); // other user

        aliceBearer = bearer("alice");
    }

    @Test
    void monthlyReportIncludesRecentTasksOnly() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/tasks").queryParam("window", "MONTHLY").build())
                .header("Authorization", aliceBearer)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationSummaries.length()").isEqualTo(2)
                .jsonPath("$.applicationSummaries[?(@.application=='app1')].completedCount").isEqualTo(2)
                .jsonPath("$.applicationSummaries[?(@.application=='app2')].completedCount").isEqualTo(1)
                .jsonPath("$.complexityDistribution[?(@.application=='app1' && @.complexity=='HIGH')].completedCount").isEqualTo(1)
                .jsonPath("$.complexityDistribution[?(@.application=='app1' && @.complexity=='MEDIUM')].completedCount").isEqualTo(1)
                .jsonPath("$.productivityTrend.length()").isEqualTo(1)
                .jsonPath("$.productivityTrend[0].completedCount").isEqualTo(3);
    }

    @Test
    void filterByApplicationAndComplexity() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/tasks")
                        .queryParam("window", "MONTHLY")
                        .queryParam("application", "app1")
                        .queryParam("complexity", "HIGH")
                        .build())
                .header("Authorization", aliceBearer)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationSummaries.length()").isEqualTo(1)
                .jsonPath("$.applicationSummaries[0].application").isEqualTo("app1")
                .jsonPath("$.applicationSummaries[0].completedCount").isEqualTo(1)
                .jsonPath("$.complexityDistribution.length()").isEqualTo(1)
                .jsonPath("$.productivityTrend.length()").isEqualTo(1)
                .jsonPath("$.productivityTrend[0].completedCount").isEqualTo(1);
    }

    @Test
    void ignoresOtherUsersData() {
        String bobBearer = bearer("bob");
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/reports/tasks").queryParam("window", "MONTHLY").build())
                .header("Authorization", bobBearer)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationSummaries.length()").isEqualTo(1)
                .jsonPath("$.applicationSummaries[0].application").isEqualTo("app1")
                .jsonPath("$.applicationSummaries[0].completedCount").isEqualTo(1);
    }

    private void insert(String userId, String app, TaskComplexity complexity, Instant closedAt) {
        TaskRecord record = new TaskRecord();
        record.setId(UUID.randomUUID());
        record.setNewEntity(true);
        record.setUserId(userId);
        record.setTitle(app + " task");
        record.setApplication(app);
        record.setComplexity(complexity);
        record.setStatus(TaskStatus.CLOSED);
        record.setClosedAt(closedAt);
        record.setCreatedAt(closedAt.minus(1, ChronoUnit.DAYS));
        repository.save(record).block();
    }

    private String bearer(String userId) {
        return jwtService.issueToken(userId, List.of("USER"))
                .map(token -> "Bearer " + token.token())
                .block();
    }
}
