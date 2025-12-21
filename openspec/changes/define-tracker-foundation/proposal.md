# Change: Define Personal Tracker foundational specs

## Why
- Establish baseline requirements for authentication, task management, reporting, and visualization so the platform can be implemented consistently across frontend and backend.
- Provide clear non-blocking, microservice-aligned guardrails before coding.

## What Changes
- Add capability deltas for user authentication, task lifecycle management with archival, reporting/analytics, and interactive visualizations.
- Clarify architecture expectations: React frontend, Spring WebFlux backend, PostgreSQL via R2DBC, non-blocking flows, Swagger/OpenAPI documentation, and TDD focus.
- Define extensibility hooks for future OAuth, notifications, exports, and role-based access without over-designing now.

## Impact
- Affected specs: auth, task-manager, reporting, visualization.
- Affected code (future implementation): authentication service, task service (CRUD + archival), reporting/analytics service, React UI for dashboards and charts.
