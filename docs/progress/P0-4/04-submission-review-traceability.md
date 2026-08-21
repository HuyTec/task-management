# P0-4 Progress 4 - Submission Review Traceability

## Delivered

- TaskReview references the exact Submission reviewed.
- Request Changes and Approve resolve the latest SUBMITTED package.
- Review cannot proceed without a submitted Evidence package.
- Legacy `/submit-review` returns Conflict and cannot bypass Evidence.
- Request Changes enables the next numbered DRAFT through CHANGES_REQUESTED.

Task-only history becomes ambiguous after Submission #2. `submission_id` makes each decision auditable against an immutable Evidence snapshot.
