package com.personal.tracker.reporting.model;

import com.personal.tracker.reporting.domain.TaskComplexity;

public record ComplexityBreakdown(String application, TaskComplexity complexity, long completedCount) {
}
