package com.personal.tracker.reporting.model;

import com.personal.tracker.reporting.domain.TaskComplexity;

public record ReportFilter(
        TimeWindow window,
        String application,
        TaskComplexity complexity,
        ReportSortField sortField,
        ReportSortDirection sortDirection
) {
}
