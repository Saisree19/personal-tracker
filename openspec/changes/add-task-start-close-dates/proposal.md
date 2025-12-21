# Change: Allow manual start/close dates when transitioning tasks

## Why
- Users want control to backfill or correct when a task was actually started or finished instead of always using "now".
- Reporting accuracy depends on correct started/closed timestamps; forcing implicit dates can misrepresent throughput.

## What Changes
- Permit clients to supply a start date when moving a task to IN_PROGRESS, defaulting to current time if omitted.
- Permit clients to supply a close date when moving a task to CLOSED, defaulting to current time if omitted.
- Validate chronology: close date must be after start date; start date must exist before closing.
- Surface start/close date inputs in UI and accept optional dates in task status update API.

## Impact
- Affected specs: task-manager.
- Affected code: task-service status update handler/DTO, task persistence for startedAt/closedAt, frontend task status controls.
