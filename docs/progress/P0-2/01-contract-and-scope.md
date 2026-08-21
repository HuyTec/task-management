# P0-2 Progress 01 - Contract and Scope

## Status

- Progress: 1/4
- State: Ready for review
- Scope: Project creation and project-member authorization
- Out of scope: Task creation, file upload, review workflow, Dashboard, deployment configuration

## Goal

Make the following flow reliable before Task assignment depends on it:

```text
Authenticated user
  -> creates Project
  -> becomes the Project OWNER in the same transaction
  -> adds an active user as a ProjectMember
  -> role rules are enforced by the backend
```

## Transaction invariant

Project creation succeeds only when both records are persisted:

```text
Project + OWNER ProjectMember = successful create
```

`ProjectService` is already transactional and creates both records in one service method. This progress will preserve that boundary and strengthen its tests instead of redesigning it.

## Authorization matrix

| Actor | Read team | Add MEMBER/VIEWER | Add MANAGER | Manage MANAGER | Manage OWNER |
|---|---:|---:|---:|---:|---:|
| OWNER | Yes | Yes | Yes | Yes | No |
| MANAGER | Yes | Yes | No | No | No |
| MEMBER | Yes | No | No | No | No |
| VIEWER | Yes | No | No | No | No |

The frontend may hide unavailable controls, but `MemberService` remains the authorization source of truth.

## Required invariants

1. A newly created Project receives an OWNER membership for its creator.
2. OWNER cannot be assigned through the member-management API.
3. The existing OWNER cannot be removed or have their role changed.
4. Only OWNER can assign or manage MANAGER.
5. MEMBER and VIEWER can read the team but cannot mutate it.
6. A deactivated or missing user cannot be added.
7. Duplicate membership is rejected; the database unique constraint remains the final concurrency guard.
8. A member with an active Task assignment cannot be removed.
9. A member with an active Task assignment cannot be changed to VIEWER.
10. Authorization happens before target-user lookup to avoid username enumeration.

## Current-code finding

Invariants 1-8 and 10 are represented in production code, although several lack direct tests. Invariant 9 is missing: `removeMember()` checks active assignments, while `updateRole()` can currently demote an active assignee to VIEWER.

This is the first expected red test for Progress 02.

## Review checklist

- [ ] The role matrix matches the intended product behavior.
- [ ] Members and viewers are allowed to read team information.
- [ ] Only OWNER can grant/manage MANAGER.
- [ ] Active assignments block removal and demotion to VIEWER.
- [ ] No file-upload or Task workflow scope is mixed into P0-2.

