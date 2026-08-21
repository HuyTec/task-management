# P0-3 Progress 3 - Frontend Single Request

## Outcome

Project Task creation now sends exactly one command:

```http
POST /api/projects/{projectId}/tasks
```

The request contains scope, ordered criteria, and optional initial assignee together.

Personal Tasks continue to use:

```http
POST /api/tasks
```

## Removed failure state

The frontend no longer loops through `addTaskCriterion` or calls `assignTask` during initial creation. Consequently, `partiallyCreatedTaskId` and the recovery link were removed from `TaskCreatePage`.

The UI now reflects the backend invariant:

```text
success = complete Project Task plan exists
failure = no Project Task plan exists
```

## Client-side guard

The acceptance-criteria editor stops adding rows at 20 and explains the limit. Backend validation remains authoritative.

## Verification

- ESLint: PASS
- Vite production build: PASS

The initial build attempt was blocked by sandbox path access while esbuild resolved `vite.config.js`; rerunning the same production build with the required filesystem permission passed.

## Mentor checkpoint

Frontend state is no longer responsible for repairing a backend transaction boundary. This is the desired responsibility split: React gathers one user intent, while Spring guarantees its atomic persistence.
