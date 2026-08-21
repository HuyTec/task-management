# P0-3 Progress 2 - Atomic Backend Command

## Outcome

`TaskWorkflowService.createProjectTask` now owns the complete Project Task planning transaction:

```text
authorize actor
  -> validate Project lifecycle and schedule
  -> resolve optional eligible assignee
  -> persist TODO Task
  -> persist ordered acceptance criteria
  -> persist optional ACTIVE/ASSIGNED assignment
  -> return TaskResponse
```

The controller performs request validation and delegates once. It does not coordinate repositories.

## Authorization order

The command resolves the current user's membership by `projectId + userId`, then requires OWNER or MANAGER. Only after that succeeds may it search for `assigneeUsername`.

This protects both mutation rights and membership privacy.

## Transaction evidence

`ProjectTaskCreationTransactionIntegrationTest` uses real H2-backed JPA repositories and the real Spring transaction interceptor. The criterion repository is spied only to raise a persistence exception after the Task insert.

Observed sequence:

```text
INSERT task
  -> simulated criterion persistence failure
  -> transaction rollback
  -> task count = 0
  -> criterion count = 0
  -> assignment count = 0
```

Result: PASS.

## Why saveAndFlush is intentional here

The command flushes before returning so database constraint failures occur inside the command transaction. A successful service response therefore cannot hide a delayed persistence failure that appears only after control returns to the client.

## Remaining boundary

This integration test proves rollback semantics using H2. PostgreSQL/Flyway runtime verification remains part of deployment smoke testing; H2 does not prove PostgreSQL migration compatibility.
