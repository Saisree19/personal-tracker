## ADDED Requirements
### Requirement: User authentication
The system SHALL require users to authenticate with credentials and issue time-bound access tokens scoped to the authenticated user.

#### Scenario: Successful login
- **WHEN** a user submits valid credentials
- **THEN** the system SHALL return an access token with expiration metadata and user context

#### Scenario: Invalid login rejected
- **WHEN** a user submits invalid credentials
- **THEN** the system SHALL reject the request with an unauthorized error and SHALL NOT issue a token

### Requirement: Per-user data isolation
The system SHALL restrict all task and reporting access to the authenticated user's data unless a future role grants broader access.

#### Scenario: Cross-user access blocked
- **WHEN** a user attempts to access another user's tasks or reports
- **THEN** the system SHALL reject the request with a forbidden error

### Requirement: Extensible authentication mechanisms
The system SHALL support pluggable authentication mechanisms so that OAuth or other identity providers can be added without breaking existing logins.

#### Scenario: Additional provider added
- **WHEN** a new authentication provider is configured
- **THEN** existing credential-based logins SHALL continue to function unchanged, and new provider logins SHALL integrate with the same user context model
