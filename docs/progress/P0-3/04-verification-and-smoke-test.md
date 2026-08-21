# P0-3 Progress 4 - Verification and Handoff

## Status

P0-3 implementation is complete in the current checkout.

## Delivered behavior

- Project Task creation is one HTTP request and one Spring transaction.
- OWNER and MANAGER may create tasks in PLANNING or ACTIVE Projects.
- MEMBER and VIEWER cannot create Project Tasks.
- Initial assignee is optional; VIEWER is ineligible.
- Criteria are required, ordered by the server, and limited to 20.
- Due date must remain inside the Project schedule.
- Generic Task creation rejects `projectId`.
- Generic Task update rejects Project relation changes.
- The dedicated unlink command remains the explicit relation-removal boundary.
- Frontend partial-creation recovery state is removed.

## Automated verification

### Backend

```text
Tests run: 118
Failures: 0
Errors: 0
Skipped: 0
```

The suite includes the JPA rollback integration test.

### Frontend

```text
ESLint: PASS
Vite production build: PASS
Modules transformed: 128
```

### Repository hygiene

`git diff --check` reported no whitespace errors. Git only reported the repository's existing LF-to-CRLF conversion warnings.

## Manual smoke test for the mentor and learner

Run these scenarios against the local PostgreSQL application before deployment:

1. OWNER creates a PLANNING Project Task with two criteria and a MEMBER assignee.
2. Open Task Detail and confirm TODO, criteria order and active assignee.
3. Create another Project Task without an assignee and confirm it is claimable.
4. Attempt creation as MEMBER and confirm 403.
5. Put the Project ON_HOLD and confirm creation is rejected.
6. Submit a due date outside the Project range and confirm 400.
7. Call `POST /api/tasks` with `projectId` and confirm 400.
8. Create a Personal Task, then call generic PATCH with `projectId`; confirm 400.

## Evidence boundary

Automated tests prove Java behavior, H2 transaction rollback, frontend lint, and frontend production compilation. They do not yet prove:

- Flyway V1-V7 migration execution on a clean PostgreSQL instance
- browser-to-backend behavior with real JWT and Redis
- Render free-tier persistence and cold-start behavior

Those checks belong to the deployment readiness phase rather than this feature's implementation claim.

## Review entry points

- `00-proposal.md`: approved architecture and product decisions
- `01-contract-and-red-tests.md`: API contract and locked invariants
- `02-atomic-backend-command.md`: transaction implementation and rollback evidence
- `03-frontend-single-request.md`: React responsibility boundary
- `04-verification-and-smoke-test.md`: final evidence and remaining runtime checks

## Mentor takeaway

Atomicity should follow the user's intent, not the number of tables. “Create a planned Project Task” is one intent, so Task, criteria, and initial assignment belong to one transaction even though they are stored separately.
