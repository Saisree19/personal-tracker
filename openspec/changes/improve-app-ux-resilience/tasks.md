## 1. Specification
- [ ] 1.1 Update task-manager spec with status validation, archived filtering, and server-driven sorting/pagination requirements
- [ ] 1.2 Add visualization spec deltas for loading and no-data/error handling
- [ ] 1.3 Add auth spec delta for JWT secret strength and startup validation
- [ ] 1.4 Add new app-shell capability spec for unified error/loading UX and session handling

## 2. Implementation (after approval)
- [ ] 2.1 Implement frontend status validation flows and archived-only view toggle
- [ ] 2.2 Implement backend task list pagination/sorting endpoints and wire frontend
- [ ] 2.3 Add shared loading/error components and toast handling for API failures
- [ ] 2.4 Enforce JWT secret entropy at auth service startup and document env sample
- [ ] 2.5 Add tests (unit/integration/UI) for the above behaviors
