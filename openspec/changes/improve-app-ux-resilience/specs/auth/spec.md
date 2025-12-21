## ADDED Requirements
### Requirement: JWT secret strength enforcement
The system SHALL validate JWT signing secrets for minimum length and entropy at startup and SHALL refuse to start with weak or missing secrets.

#### Scenario: Startup blocked on weak secret
- **WHEN** the auth service starts with a missing or weak JWT secret
- **THEN** the service SHALL fail fast with a clear error message and SHALL NOT issue any tokens
