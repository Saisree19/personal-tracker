## ADDED Requirements
### Requirement: Task creation and fields
The system SHALL allow a user to create tasks with title, description, application/project name, complexity rating (LOW|MEDIUM|HIGH|VERY_HIGH), deadline date, status (OPEN|IN_PROGRESS|CLOSED), and created date.

#### Scenario: Create task with required attributes
- **WHEN** a user submits a new task with all required fields
- **THEN** the system SHALL persist the task with status OPEN (unless explicitly provided) and SHALL record the created date

### Requirement: Task editing and notes
The system SHALL allow users to edit task fields and append notes without overwriting existing notes; each note SHALL capture timestamp and author.

#### Scenario: Append note preserves history
- **WHEN** a user adds a note to a task
- **THEN** the system SHALL append the note with timestamp and author while preserving prior notes

### Requirement: Status transitions and archival
The system SHALL allow status updates; when a task transitions to CLOSED it SHALL capture the closed date and move the task to an archived collection that is read-only.

#### Scenario: Close task moves to archive
- **WHEN** a user marks a task as CLOSED
- **THEN** the system SHALL record the closed date, move the task to archived tasks, and prevent further edits to the archived record

### Requirement: Ownership enforcement
The system SHALL ensure users can create, update, and view only their own tasks unless broader roles are introduced later.

#### Scenario: Prevent editing another user's task
- **WHEN** a user attempts to edit a task they do not own
- **THEN** the system SHALL reject the request with a forbidden error and SHALL NOT modify the task
