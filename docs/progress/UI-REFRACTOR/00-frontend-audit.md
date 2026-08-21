# UI Refactor — Progress 0: Frontend Audit

## Scope reviewed

- All React routes and pages, including administration pages.
- Shared layout, authentication, task, expense and project components.
- All frontend API modules and formatting utilities.
- The complete global stylesheet and Vite/ESLint configuration.

## Architecture before refactor

```text
App routes
├── Authentication pages
├── User pages → AppHeader → WorkspaceHeader
├── Admin pages → AdminHeader → WorkspaceHeader
└── Feature components → API modules → Spring Boot API
```

## Findings

- Global CSS combines tokens, primitives and feature styling in one layer.
- Buttons, feedback messages and empty/loading states are implemented with several unrelated patterns.
- Navigation omits History and exact route matching hides the active state on nested routes.
- Dense tables remain desktop-first; wrappers prevent page overflow but do not provide a good mobile reading order.
- Large feature pages combine data orchestration and presentation. API/state ownership is retained to avoid changing workflow behavior; only stable UI responsibilities will be extracted.
- Authorization conditions in Task, Project and Member pages are sensitive boundaries. They must remain unchanged and are not security controls by themselves.
- Google authentication correctly sends the Google credential to the backend and stores only the application access token. The missing-client configuration state needs clearer copy.

## Approved boundary

No backend, API contract, route, authentication transport, workflow transition or role rule will be changed.
