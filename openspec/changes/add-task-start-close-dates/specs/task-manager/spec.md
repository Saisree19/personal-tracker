## MODIFIED Requirements
### Requirement: Status transitions and archival
The system SHALL allow status updates; when a task transitions to CLOSED it SHALL capture the closed date and move the task to an archived collection that is read-only. When a task is moved to IN_PROGRESS the system SHALL record a start date (provided by the client or defaulting to the current time). When a task is moved to CLOSED the system SHALL record a close date (provided by the client or defaulting to the current time) and SHALL require that the start date exists and the close date is after the start date.

#### Scenario: Start task with provided start date
- **WHEN** a user moves a task to IN_PROGRESS and supplies a start date
- **THEN** the system SHALL set the task status to IN_PROGRESS, persist the provided start date, and default to current time only when the client omits the start date

#### Scenario: Close task with provided close date
- **WHEN** a user moves a task from IN_PROGRESS to CLOSED and supplies a close date (and optional start date)
- **THEN** the system SHALL persist the close date, default close date to current time if omitted, require that a start date exists, and archive the task as read-only

#### Scenario: Reject close when close date is not after start
- **WHEN** a user attempts to close a task with a close date that is on/before the start date
- **THEN** the system SHALL reject the request and SHALL NOT change the task status
