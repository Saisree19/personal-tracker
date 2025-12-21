## 1. Backend
- [ ] Add email field and uniqueness to user model/repository; migrate/seed demo with email.
- [ ] Implement register endpoint (username, email required, password hash, issue JWT on success).
- [ ] Implement forgot-password endpoint (idempotent response) that creates a time-bound reset token; if SMTP configured, send link to frontend base URL; else write token+email to a dev mail sink for retrieval.
- [ ] Implement OTP fallback: generate short-lived OTP alongside token when SMTP unavailable; add verify/reset endpoint that accepts token or OTP and sets new password; invalidate outstanding reset artifacts after use.
- [ ] Add config/env for SMTP creds and frontend reset base URL; add tests covering success/invalid/expired/used flows and OTP path.

## 2. Frontend
- [ ] Add Create Account screen (email mandatory) and route/link from login; call register endpoint and log the user in on success.
- [ ] Add Forgot Password screen to request reset via email; show success regardless of lookup; surface dev mail sink/OTP when returned in dev mode.
- [ ] Add Reset Password screen that accepts token (from link) or OTP + email, posts new password, then routes to login.
- [ ] Update login UI to link to signup and forgot screens; add client-side validation and error/success messaging.

## 3. Operations & DX
- [ ] Document new env vars (SMTP, frontend reset URL, mail sink toggle) and local/dev flows.
- [ ] Update OpenAPI docs for new auth endpoints and flows.
