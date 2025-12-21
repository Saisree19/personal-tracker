# Change: Add email signup and reset flows

## Why
- Allow users to create accounts themselves with mandatory email instead of only the seeded demo user.
- Provide a password reset path that uses email links pointing to the frontend, with an OTP-based fallback when SMTP is unavailable.
- Keep the reset process secure (time-bound tokens/OTPs, one-time use, generic responses) while working in dev environments without outbound email.

## What Changes
- Extend auth service to store email, enforce uniqueness, and expose register + forgot/reset endpoints using signed, expiring tokens or OTPs.
- Add a configurable mail sink for development (capture reset links/OTPs) and support SMTP when provided; reset links target the frontend base URL.
- Add frontend screens for Create Account, Forgot Password, and Reset/OTP entry, wired to the new endpoints and flows.

## Impact
- Affected specs: auth
- Affected code: auth-service domain/repository/controller/service, auth-service config for email/OTP delivery, frontend auth UI (login/register/forgot/reset), environment configuration for frontend reset base URL and SMTP/dev mail sink.
