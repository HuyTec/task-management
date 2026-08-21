# P0-2 Progress 04 - Verification and Smoke-Test Guide

## Status

- Progress: 4/4
- Automated verification: Passed
- Runtime browser/PostgreSQL smoke test: Not run
- P0-2 code state: Ready for review

## Automated evidence

### Backend

```text
Tests run: 112, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The final `ProjectMemberServiceTest` matrix contains 17 passing invocations, including separate MEMBER and VIEWER authorization cases.

### Frontend

```text
npm.cmd run lint  -> passed
npm.cmd run build -> passed
Vite transformed 128 modules
```

### Source hygiene

```text
git diff --check -> no whitespace errors
```

Git printed Windows LF/CRLF conversion warnings. These are line-ending notices, not `diff --check` failures.

## P0-2 changed files

- `backend/src/main/java/com/taskmanagement/service/project/MemberService.java`
- `backend/src/test/java/com/taskmanagement/service/ProjectMemberServiceTest.java`
- `docs/progress/P0-2/01-contract-and-scope.md`
- `docs/progress/P0-2/02-red-tests.md`
- `docs/progress/P0-2/03-minimal-fix.md`
- `docs/progress/P0-2/04-verification-and-smoke-test.md`

The checkout also contains uncommitted P0-1 Auth changes. They were preserved and were not rewritten during P0-2.

## Manual smoke-test prerequisites

- Backend running with PostgreSQL and Redis.
- Frontend running against that backend.
- Two active accounts:
  - Account A: prospective OWNER
  - Account B: prospective MEMBER

## Manual smoke-test flow

### 1. Create Project

1. Sign in as Account A.
2. Create a Project with a valid date range.
3. Open the Project detail page.
4. Confirm Account A is displayed as OWNER.

Expected: the Project and OWNER membership both exist.

### 2. Add and inspect a MEMBER

1. From the team page, add Account B as MEMBER.
2. Confirm Account B appears once.
3. Attempt to add Account B again.

Expected: first request succeeds; duplicate request returns conflict and creates no second membership.

### 3. Verify read-only role

1. Sign out Account A and sign in as Account B.
2. Open the Project and team page.
3. Confirm Account B can read the member list.
4. Confirm member-management controls are absent.

Expected: read succeeds; mutation remains forbidden by backend even if an API request is crafted manually.

### 4. Verify active-assignment guard

1. Sign in as Account A.
2. Create and assign a Project Task to Account B.
3. Attempt to change Account B from MEMBER to VIEWER.
4. Attempt to remove Account B.

Expected responses:

```text
Reassign active tasks before changing this project member to VIEWER
Reassign active tasks before removing this project member
```

5. Reassign or end the active Task assignment.
6. Change Account B to VIEWER again.

Expected: the role change now succeeds.

### 5. Verify manager boundary

1. As OWNER, promote a third account to MANAGER.
2. As that MANAGER, add a MEMBER and a VIEWER.
3. Attempt to add or manage another MANAGER.

Expected: regular-member operations succeed; peer-manager operations return forbidden.

## Review order

1. Review `01-contract-and-scope.md` for product rules.
2. Review `02-red-tests.md` for proof of the missing behavior.
3. Review the `MemberService` diff and `03-minimal-fix.md` together.
4. Use this file to reproduce automated and manual verification.

## Remaining boundary

P0-2 is complete at code and automated-test level. It should not be called runtime-verified until the manual flow above runs against a real PostgreSQL/Redis environment or the Render preview deployment.

