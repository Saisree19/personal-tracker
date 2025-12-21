# Context
- Personal Tracker targets microservices with React frontend and Spring WebFlux backend using PostgreSQL via R2DBC.
- Requirements emphasize non-blocking flows, clean layering (Controller → Service → Repository), standardized API errors, and OpenAPI/Swagger documentation.
- Future extensibility for OAuth, notifications, exports, and RBAC is desired without blocking initial delivery.

# Goals / Non-Goals
- Goals: Define capability boundaries, non-blocking constraints, and data/UX expectations for initial implementation.
- Non-Goals: Detailed schema design, API routing, UI component breakdown, or performance tuning beyond reactive, low-latency defaults.

# Decisions
- Capability separation: auth, task-manager, reporting, visualization. Reporting covers data aggregation; visualization focuses on UI chart behavior.
- Data ownership: Tasks and notes stored in task service with user ownership; archived tasks become read-only records.
- Reactive mandate: All backend IO uses WebFlux + R2DBC; prohibit blocking calls in pipelines.
- Documentation: Every public API described with OpenAPI annotations; standardized error payloads.
- Extensibility hooks: Keep auth pluggable for future OAuth; reporting/visualization filters accept additive dimensions; notifications/exports deferred but not blocked.

# Risks / Trade-offs
- Tight scope may defer advanced analytics (e.g., burnup/velocity) to later changes; mitigated by filter-friendly reporting requirements.
- Archival read-only constraint limits task edits post-close; reopen flows are intentionally out-of-scope for clarity.

# Open Questions
- Preferred identity provider for future OAuth? (e.g., Okta vs. self-managed)
- Any SLAs for report generation latency or data freshness windows?
