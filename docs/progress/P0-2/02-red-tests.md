# P0-2 Progress 02 - Red Tests

## Status

- Progress: 2/4
- State: Ready for review
- Method: Test-first verification
- Production code changed in this progress: No

## Test coverage added

`ProjectMemberServiceTest` now covers these boundaries:

1. OWNER and VIEWER can read the team.
2. OWNER can add MANAGER.
3. MANAGER can add MEMBER.
4. MANAGER cannot add MANAGER.
5. OWNER cannot be assigned through the member API.
6. MEMBER cannot add another member.
7. Authorization rejects the actor before target-user lookup.
8. Missing or deactivated users cannot be added.
9. Duplicate membership is rejected.
10. OWNER cannot be removed or have their role changed.
11. MANAGER cannot manage a peer MANAGER.
12. Active assignees cannot be removed.
13. Active assignees cannot be changed to VIEWER.
14. A MEMBER without active assignments can be changed to VIEWER.

## Red-test evidence

Focused command:

```powershell
mvn -Dtest=ProjectMemberServiceTest,ProjectServiceTest test
```

Observed result before changing production code:

```text
Tests run: 23, Failures: 1, Errors: 0, Skipped: 0

ProjectMemberServiceTest.updateRoleRejectsViewerDemotionForActiveAssignee
Expecting code to raise a throwable.
```

The other 22 focused tests passed. The single failure confirms that the existing `updateRole()` method does not query active Task assignments before changing a MEMBER to VIEWER.

## Why this is a P0 defect

The Task workflow allows MEMBER to execute assigned work and prevents VIEWER from receiving work. Demoting an active assignee to VIEWER produces contradictory state:

```text
ACTIVE assignment + VIEWER assignee
```

The assignee can no longer complete the workflow, while the assignment still blocks ordinary removal. The Task can become operationally stuck.

## Expected production change

Only when the target role is VIEWER:

1. Query whether the membership owns any ACTIVE assignment.
2. Reject the change with `BadRequestException` when one exists.
3. Do not mutate the entity before this guard passes.
4. Do not add the query for MEMBER or MANAGER target roles.

## Review checklist

- [ ] The red test represents a business invariant, not an implementation detail.
- [ ] Exactly one production behavior is missing.
- [ ] Authorization is tested before username lookup.
- [ ] The planned fix does not broaden P0-2 scope.

