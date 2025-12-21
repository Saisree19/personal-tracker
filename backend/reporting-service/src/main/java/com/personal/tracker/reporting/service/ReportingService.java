package com.personal.tracker.reporting.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.personal.tracker.reporting.domain.TaskComplexity;
import com.personal.tracker.reporting.domain.TaskRecord;
import com.personal.tracker.reporting.model.ApplicationSummary;
import com.personal.tracker.reporting.model.ComplexityBreakdown;
import com.personal.tracker.reporting.model.ReportFilter;
import com.personal.tracker.reporting.model.ReportResponse;
import com.personal.tracker.reporting.model.ReportSortDirection;
import com.personal.tracker.reporting.model.ReportSortField;
import com.personal.tracker.reporting.model.StatusSummary;
import com.personal.tracker.reporting.model.TimeWindow;
import com.personal.tracker.reporting.model.TrendPoint;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReportingService {

    private final R2dbcEntityTemplate template;

    public ReportingService(R2dbcEntityTemplate template) {
        this.template = template;
    }

    public Mono<ReportResponse> generateReport(String userId, ReportFilter filter) {
        Instant now = Instant.now();
        Instant start = windowStart(now, filter.window());

        Criteria criteria = Criteria.where("user_id").is(userId);

        if (filter.application() != null && !filter.application().isBlank()) {
            criteria = criteria.and("application").is(filter.application());
        }
        if (filter.complexity() != null) {
            criteria = criteria.and("complexity").is(filter.complexity());
        }

        Sort sort = buildSort(filter.sortField(), filter.sortDirection());
        Query query = Query.query(criteria).sort(sort);

        return template.select(TaskRecord.class)
            .matching(query)
            .all()
            .collectList()
            .map(tasks -> aggregate(tasks, filter.window(), start, now));
    }

        private ReportResponse aggregate(List<TaskRecord> tasks, TimeWindow window, Instant start, Instant now) {
        List<TaskRecord> inWindow = tasks.stream()
            .filter(task -> isInWindow(task, start, now))
            .toList();

        List<ApplicationSummary> appSummaries = inWindow.stream()
            .collect(Collectors.groupingBy(TaskRecord::getApplication, Collectors.counting()))
            .entrySet()
            .stream()
            .map(e -> new ApplicationSummary(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(ApplicationSummary::application))
            .toList();

        List<ComplexityBreakdown> complexitySummaries = inWindow.stream()
            .collect(Collectors.groupingBy(task -> Map.entry(task.getApplication(), task.getComplexity()), Collectors.counting()))
            .entrySet()
            .stream()
            .map(e -> new ComplexityBreakdown(e.getKey().getKey(), e.getKey().getValue(), e.getValue()))
            .sorted(Comparator
                .comparing(ComplexityBreakdown::application)
                .thenComparing(cb -> cb.complexity().ordinal()))
            .toList();

        List<StatusSummary> statusSummaries = inWindow.stream()
            .collect(Collectors.groupingBy(task -> task.getStatus().name(), Collectors.counting()))
            .entrySet()
            .stream()
            .map(e -> new StatusSummary(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(StatusSummary::status))
            .toList();

        List<TrendPoint> trend = inWindow.stream()
            .filter(task -> task.getClosedAt() != null)
            .collect(Collectors.groupingBy(task -> bucketStart(task.getClosedAt(), window), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TrendPoint(e.getKey(), e.getValue()))
            .toList();

        return new ReportResponse(appSummaries, complexitySummaries, trend, statusSummaries);
    }

        private boolean isInWindow(TaskRecord task, Instant start, Instant now) {
        Instant effective = task.getClosedAt();
        if (effective == null) {
            effective = task.getArchivedAt();
        }
        if (effective == null) {
            effective = task.getCreatedAt();
        }
        if (effective == null) {
            return false;
        }
        return !effective.isBefore(start) && !effective.isAfter(now);
        }

    private Sort buildSort(ReportSortField field, ReportSortDirection direction) {
        String property = field == ReportSortField.COMPLEXITY ? "complexity" : "closed_at";
        Sort.Order order = direction == ReportSortDirection.ASC
                ? Sort.Order.asc(property)
                : Sort.Order.desc(property);
        return Sort.by(order);
    }

    private Instant windowStart(Instant now, TimeWindow window) {
        return switch (window) {
            case WEEKLY -> now.minusSeconds(7 * 24 * 3600L);
            case MONTHLY -> now.atOffset(ZoneOffset.UTC).minusMonths(1).toInstant();
            case QUARTERLY -> now.atOffset(ZoneOffset.UTC).minusMonths(3).toInstant();
            case HALF_YEARLY -> now.atOffset(ZoneOffset.UTC).minusMonths(6).toInstant();
            case YEARLY -> now.atOffset(ZoneOffset.UTC).minusYears(1).toInstant();
        };
    }

    private LocalDate bucketStart(Instant closedAt, TimeWindow window) {
        LocalDate date = closedAt.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (window) {
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
            case QUARTERLY -> date.withMonth(((date.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
            case HALF_YEARLY -> date.withMonth(date.getMonthValue() <= 6 ? 1 : 7).withDayOfMonth(1);
            case YEARLY -> date.withDayOfYear(1);
        };
    }
}
