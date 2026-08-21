# P0-2 Progress 03 - Minimal Production Fix

## Status

- Progress: 3/4
- State: Ready for review
- Focused tests: Green

## Production change

`MemberService` now uses one `requireNoActiveAssignments()` guard for two destructive membership changes:

```text
remove ProjectMember
  -> reject when ACTIVE assignment exists

change ProjectMember role to VIEWER
  -> reject when ACTIVE assignment exists
```

The role is mutated only after the guard passes. Target roles MEMBER and MANAGER do not trigger the assignment query.

## Why the guard is shared

Removal and VIEWER demotion have the same prerequisite: active work must first be reassigned. Centralizing the repository query prevents the two mutation paths from drifting apart again.

## Additional cleanup

The peer-manager authorization message now says:

```text
Only project owner can manage project managers
```

This replaces the stale word `admins` and matches the current `ProjectRole.MANAGER` enum. It does not change authorization behavior.

## Green-test evidence

Focused command:

```powershell
mvn -Dtest=ProjectMemberServiceTest,ProjectServiceTest test
```

Result:

```text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Breakdown:

- `ProjectMemberServiceTest`: 15 passed
- `ProjectServiceTest`: 12 passed

## Review checklist

- [ ] `membership.setRole()` occurs after the active-assignment guard.
- [ ] Assignment lookup runs only for VIEWER demotion and member removal.
- [ ] The existing OWNER/MANAGER authorization order is unchanged.
- [ ] No Task workflow or frontend behavior was redesigned.

