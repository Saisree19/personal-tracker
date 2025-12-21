## ADDED Requirements
### Requirement: Status validation and date defaults
The system SHALL enforce start/close validation rules and sensible defaults: a task SHALL NOT be closed without a recorded start date, the close date SHALL be after the start date, and missing start/close dates SHALL default to today only when consistent with these rules.

#### Scenario: Close requires start date
- **WHEN** a user attempts to close a task that has no recorded start date
- **THEN** the system SHALL reject the transition, explain that a start date is required, and SHALL NOT change the status

#### Scenario: Close date must follow start
- **WHEN** a user provides a close date that is on or before the start date
- **THEN** the system SHALL reject the transition and SHALL NOT update the task status

#### Scenario: Default dates applied safely
- **WHEN** a user starts a task without providing a start date or closes a task without providing a close date
- **THEN** the system SHALL default the missing date to today only if it preserves start-before-close ordering, and SHALL record the applied defaults

### Requirement: Archived view filtering
The system SHALL provide a toggle to view archived (closed) tasks; when enabled, the task list SHALL return only closed/archived tasks, and when disabled, it SHALL exclude them. Sorting, pagination, and empty-state messaging SHALL apply to the filtered set.

#### Scenario: Archived-only view
- **WHEN** a user enables the archived filter
- **THEN** the system SHALL show only closed tasks, update pagination based on that filtered list, and display a specific "no archived tasks" message if none exist

### Requirement: Server-driven task list paging and sorting
The system SHALL support server-driven pagination and sorting for tasks, accepting sort field/direction and page parameters, and SHALL return page metadata so the UI avoids re-sorting full collections client-side.

#### Scenario: Sorted, paged task list
- **WHEN** a user requests tasks with a sort field/direction and page parameters
- **THEN** the system SHALL return only the requested page of tasks in that order along with total pages/count metadata
