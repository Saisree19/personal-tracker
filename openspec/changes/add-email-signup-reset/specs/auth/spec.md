## ADDED Requirements

### Requirement: Self-service account creation with email
The system SHALL allow users to create an account with mandatory email, username, and password, enforcing unique email and username, storing passwords as hashes, and issuing a JWT on successful registration.

#### Scenario: Successful signup
- **WHEN** a user submits a valid email, unique username, and strong password
- **THEN** the system creates the user, hashes the password, and returns an authentication token

#### Scenario: Duplicate email or username
- **WHEN** a user submits an email or username that already exists
- **THEN** the system rejects the request with a validation error and does not create a new account

### Requirement: Password reset via email link pointing to frontend
The system SHALL accept a forgot-password request with an email, generate a signed, single-use reset token with an expiration, and send a reset link that targets the configured frontend base URL. Responses SHALL NOT reveal whether the email exists.

#### Scenario: Email reset link issued
- **WHEN** a user requests a reset with a valid email and SMTP is configured
- **THEN** the system sends a reset link containing the token to the email, stores the token with expiration, and returns a generic success response

### Requirement: OTP fallback when email delivery unavailable
The system SHALL support a fallback that issues a short-lived OTP (e.g., 6–8 digits) alongside the reset token when SMTP is not configured, exposing the OTP/reset artifact via a development-safe mail sink for local use.

#### Scenario: OTP path without SMTP
- **WHEN** SMTP is not configured and a user requests a reset
- **THEN** the system creates a reset token and OTP, records them in the mail sink for retrieval, and returns a generic success response

### Requirement: Secure reset confirmation
The system SHALL allow password reset using a valid reset token or OTP, require the email, enforce expiration and single-use, invalidate related reset artifacts after success, and hash the new password.

#### Scenario: Successful reset
- **WHEN** a user submits a valid reset token or OTP with their email and a new password before expiration
- **THEN** the system sets the new password, invalidates outstanding reset tokens/OTPs for that user, and returns a success response

#### Scenario: Expired or invalid reset attempt
- **WHEN** a user submits an expired, invalid, or already-used token/OTP
- **THEN** the system rejects the reset with a generic error and leaves the password unchanged

### Requirement: Frontend flows for signup and reset
The frontend SHALL provide screens for Create Account, Forgot Password, and Reset Password (token/OTP entry) that call the corresponding APIs, treat unknown emails as success, and route back to login after a successful reset.

#### Scenario: User completes reset via link
- **WHEN** a user opens the reset link from email (frontend URL with token) and submits a new password
- **THEN** the frontend calls the reset endpoint with the token and email, shows success, and directs the user to login
