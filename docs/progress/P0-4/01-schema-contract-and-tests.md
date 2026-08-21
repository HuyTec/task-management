# P0-4 Progress 1 - Schema, Contract and Red-Test Boundaries

## Delivered

- V8 migration for `task_submissions`, `task_evidences`, and `task_reviews.submission_id`.
- Submission state: DRAFT or SUBMITTED.
- Evidence types: UPLOADED_FILE, GITHUB_COMMIT, GITHUB_PR, EXTERNAL_LINK.
- Upload state: PENDING, READY, FAILED.
- One DRAFT per Task is protected by a PostgreSQL partial unique index.
- Submission sequence number is unique per Task.

## Important migration choice

`task_reviews.submission_id` remains nullable for historical V6 review records. New review decisions created by P0-4 always reference the exact submitted Submission.

## Locked tests

- Another Task's assignee receives Forbidden and no Submission is saved.
- File size above 25 MiB receives BadRequest before storage or persistence.
- Evidence number 11 receives BadRequest before persistence.
- ConflictException maps to HTTP 409.
- A JPA integration test persists a SUBMITTED Evidence, attempts deletion, receives Conflict, and proves the Evidence remains stored.

## Result

Required targeted tests: PASS.
