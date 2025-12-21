## ADDED Requirements
### Requirement: Unified loading and error handling
The system SHALL standardize loading and error presentation across the UI using shared components (e.g., toasts for non-blocking errors, inline messages for form errors, skeletons/spinners for loading states) without blocking other interactions.

#### Scenario: API failure surfaced non-blockingly
- **WHEN** an API request fails while the user is viewing tasks or reports
- **THEN** the system SHALL show a toast with the error, keep the UI interactive, and allow retry without a full page reload

### Requirement: Auth session handling
The system SHALL clear local tokens and prompt re-authentication on 401 responses, and SHALL restore tokens only from trusted storage on load.

#### Scenario: Session expired during use
- **WHEN** the backend returns 401 for an authenticated request
- **THEN** the system SHALL clear stored credentials, show a brief session-expired notice, and route the user to sign in

### Requirement: Environment configuration discoverability
The system SHALL provide documented configuration defaults and a checked-in example environment file for required frontend/backend variables so deployments can be configured consistently.

#### Scenario: Example configuration available
- **WHEN** a contributor sets up the project
- **THEN** they SHALL find an example environment file listing required variables (including service URLs and JWT secret), reducing setup errors
