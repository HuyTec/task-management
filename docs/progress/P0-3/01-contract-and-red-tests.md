# P0-3 Progress 1 - Contract and Invariant Tests

## Outcome

The Project Task creation boundary is now explicit:

```http
POST /api/projects/{projectId}/tasks
```

The generic endpoint rejects a non-null `projectId`:

```http
POST /api/tasks
```

This prevents direct API callers from bypassing the complete Project Task planning contract.

## Request contract

- title: required, trimmed by the command, maximum 160 characters
- description: required, maximum 2000 characters
- priority: required
- dueDate: required
- criteria: 1-20 non-blank entries, maximum 1000 characters each
- assigneeUsername: optional, maximum 255 characters

## Locked invariants

- Only OWNER or MANAGER can create a Project Task.
- Authorization happens before assignee lookup.
- PLANNING and ACTIVE accept new tasks.
- ON_HOLD, COMPLETED and ARCHIVED reject new tasks.
- Due date must remain inside the Project schedule.
- Criteria positions are generated from request order.
- VIEWER cannot be assigned.
- New tasks start in TODO.
- Initial assignment uses ASSIGNED and ACTIVE.

## Tests added or updated

- Complete Project Task command builds Task, ordered criteria and assignment.
- MEMBER is rejected before assignee lookup and persistence.
- Closed lifecycle rejects persistence.
- Out-of-range due date rejects persistence.
- VIEWER cannot be the initial assignee.
- Generic Task creation rejects `projectId` before Project lookup.

## Verification

Command:

```powershell
mvn -q '-Dtest=TaskWorkflowServiceTest,TaskServiceTest' test
```

Result: PASS.

Environment note: the repository wrapper currently fails inside its embedded PowerShell bootstrap, so verification used the already-installed Maven 3.9.9 distribution and the existing local dependency cache.

## Mentor checkpoint

The controller does not orchestrate Task, criteria, or assignment repositories. It delegates one business command. This is the boundary that allows `@Transactional` to represent the whole user intent rather than one database write.
