package com.personal.tracker.reporting.model;

import java.util.List;

public record ReportResponse(
        List<ApplicationSummary> applicationSummaries,
        List<ComplexityBreakdown> complexityDistribution,
        List<TrendPoint> productivityTrend,
        List<StatusSummary> statusDistribution
) {
}
