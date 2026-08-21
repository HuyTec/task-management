# UI Refactor — Progress 2: Authentication and Application Shell

## Delivered

- Added `AppShell` as the shared user-workspace layout boundary.
- Added History to the primary navigation.
- Nested Task, Expense and Project routes now keep their navigation item active.
- Replaced the mobile horizontal navigation with an explicit accessible menu using `aria-expanded` and `aria-controls`.
- Preserved username/password, refresh-cookie and Google ID token flows.
- Replaced the Google 409 linking block with a dedicated confirmation panel. Linking still requires the existing password.
- Missing `VITE_GOOGLE_CLIENT_ID` now renders a clear configuration status instead of an inert sign-in imitation.
- Improved password help/error semantics and preserved official Google-rendered button proportions.

## Verification

- ESLint: passed with zero warnings.
- Vite production build: passed, 135 modules transformed.
- The first sandboxed build was blocked by esbuild directory access; the approved build execution passed.
- Backend and API contracts: unchanged.

## Next

Apply the shared shell and responsive content patterns to Task, Expense, Project and Profile feature pages.
