# Change: Improve UX clarity and resilience

## Why
- Users need clearer validation and feedback when changing task status (start/close) and when viewing archived tasks.
- Inconsistent error and loading handling across the UI makes failures opaque and slows recovery.
- Missing guardrails around configuration, pagination, and secret strength risk reliability and security.

## What Changes
- Add UX and validation requirements for task start/close flows, archived filtering, and server-driven sorting/pagination.
- Add frontend shell requirements for consistent loading, error surfacing, and authenticated session handling.
- Add visualization/reporting UX requirements for loading and no-data states.
- Add security/config requirements (JWT secret strength and documented env expectations).

## Impact
- Affected specs: task-manager, visualization, auth, app-shell (new).
- Affected code: React task table and status handlers, task service list/sort API, auth startup validation, shared UI error/loading components.
