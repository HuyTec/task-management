# P0-4 Approved Proposal - Task Evidence and Submission Review

## Status

- Approved for implementation
- Independent from P0-3 atomic Project Task creation
- Storage provider decision is preserved, but the current checkout contains no concrete S3/Cloudinary adapter or configuration

## State model

```text
Task IN_PROGRESS
  -> Submission DRAFT (may be empty)
  -> add up to 10 READY evidences
  -> Submission SUBMITTED + Task IN_REVIEW (one transaction)
  -> APPROVED: Task DONE
  -> REQUEST_CHANGES: Task CHANGES_REQUESTED
  -> assignee resumes work and creates the next DRAFT Submission
```

No second Submission may be created while the Task is IN_REVIEW. A SUBMITTED Submission and its Evidence are immutable.

## Authorization

- Only the active assignee creates a Submission.
- Only the Submission owner and an eligible OWNER/MANAGER reviewer may read Evidence.
- A reviewer who is also the assignee may read but may not review their own work.
- Authorization precedes Submission/Evidence detail disclosure where practical.

## Submit conditions

- Submission is DRAFT.
- Submission belongs to the active assignee.
- Task is IN_PROGRESS or CHANGES_REQUESTED.
- At least one Evidence exists.
- Every file Evidence is READY; PENDING or FAILED uploads block submission.
- Acceptance criteria do not need to be satisfied before submission; the reviewer evaluates them afterward.

## Limits

- 25 MiB per uploaded file.
- 10 Evidence records per Submission.
- Allowed uploaded extensions: pdf, docx, pptx, xlsx, png, jpg, zip.
- ZIP files are stored only; backend never extracts or executes them.
- Folder v1 is a client-created ZIP Evidence.

## API contract

```http
POST   /api/tasks/{taskId}/submissions
GET    /api/tasks/{taskId}/submissions
GET    /api/submissions/{submissionId}
POST   /api/submissions/{submissionId}/evidences/links
DELETE /api/submissions/{submissionId}/evidences/{evidenceId}
POST   /api/submissions/{submissionId}/evidences/files/initiate
POST   /api/submissions/{submissionId}/evidences/files/complete
POST   /api/submissions/{submissionId}/submit
```

The old task-level submit-review command must not create an evidence-free review cycle after P0-4 integration.

## Review traceability

New TaskReview records reference the exact Submission under review. The migration keeps `submission_id` nullable only for historical review rows created before P0-4.

## Storage boundary

Application/domain logic depends on an `EvidenceStorage` port. A concrete presigned URL adapter cannot be selected from the current checkout because no provider-specific dependency or configuration is present. P0-4 will not guess S3 versus Cloudinary.
