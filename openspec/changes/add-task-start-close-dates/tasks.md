## 1. Implementation
- [ ] 1.1 Update task-service status API to accept optional startDate/closeDate, defaulting to now, and validate closeDate > startDate
- [ ] 1.2 Persist startedAt/closedAt changes and ensure archived tasks remain read-only
- [ ] 1.3 Add tests for start/close with provided dates and validation failures
- [ ] 1.4 Update frontend task row to let users choose start and close dates before triggering transitions; keep sensible defaults
- [ ] 1.5 Verify reporting uses the stored closedAt/startDate values (no changes expected beyond data correctness)
