package com.personal.tracker.reporting.model;

import java.time.LocalDate;

public record TrendPoint(LocalDate periodStart, long completedCount) {
}
