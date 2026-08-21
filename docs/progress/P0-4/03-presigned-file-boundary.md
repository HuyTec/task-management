# P0-4 Progress 3 - Presigned File Boundary

## Implemented provider-neutral boundary

```http
POST /api/submissions/{submissionId}/evidences/files/initiate
POST /api/submissions/{submissionId}/evidences/files/complete
```

`EvidenceStorage` defines provider operations to create a presigned upload contract and verify the object before marking Evidence READY.

Validation before provider access includes owner/DRAFT checks, a serialized 10-Evidence limit, positive size capped at 25 MiB, allowed extensions, and a server-generated storage key.

## Current blocker

The checkout contains no S3 or Cloudinary dependency, configuration, adapter, or prior presigned contract. A safe fallback returns HTTP 503 instead of pretending upload succeeded.

The concrete adapter needs the approved provider identity. S3 presigned PUT and Cloudinary signed upload have materially different request and verification contracts, so selecting one by assumption would be unsafe.

## Status

- Domain/API port: complete and compiling.
- Concrete provider adapter: awaiting provider identity/configuration.
- Browser file/folder upload: intentionally not connected to a fake provider.
