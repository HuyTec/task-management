# P0-4 Progress 2 - Submission and Link Evidence

## Delivered commands

```http
POST   /api/tasks/{taskId}/submissions
GET    /api/tasks/{taskId}/submissions
GET    /api/submissions/{submissionId}
POST   /api/submissions/{submissionId}/evidences/links
DELETE /api/submissions/{submissionId}/evidences/{evidenceId}
POST   /api/submissions/{submissionId}/submit
```

## Invariants

- Only the active assignee creates and submits work.
- Submission creation is limited to IN_PROGRESS or CHANGES_REQUESTED.
- A pessimistic Submission lock serializes evidence count and mutation checks.
- SUBMITTED Evidence is immutable and returns Conflict.
- Assignee sees their Submission; OWNER/MANAGER may inspect it as reviewers; ordinary unrelated members do not.
- Submit requires at least one Evidence and no non-READY file upload.
- Submission SUBMITTED and Task IN_REVIEW change in one transaction.

## Link validation

- Only absolute HTTP(S) URLs are accepted.
- GitHub commit/PR Evidence requires GITHUB provider and a github.com host.
- Link Evidence cannot claim OBJECT_STORAGE provider.
