# Single checkout verification — 17 November 2026

All commands ran from the consolidated `baiti` checkout on `igumi`.

| Check | Result |
| --- | --- |
| Source comparison | Backend and frontend files matched the sibling checkout byte for byte, including the local P07/P08 changes. |
| Backend unit and architecture tests | 151 passed. |
| Fresh PostgreSQL database | All 33 migrations applied and all seven migration checks passed. |
| Full backend verification | 179 PostgreSQL integration tests passed, including eight tenant-isolation tests. Maven reported build success with zero failures, errors or skips. |
| Frontend | API type generation, ESLint, TypeScript checking, all eight Vitest tests and the Vite production build passed. |
| Git | `git diff --check` passed, the migration files had an empty diff, and Git listed one worktree. |

The previous native runner settings file was absent. Verification used a
dedicated local PostgreSQL 16 cluster with the separate `baiti_migrator` and
`baiti_app` roles. The cluster was stopped after verification. The offline
frontend install needed uncached packages, so `npm ci` was rerun with registry
access against the existing lockfile.

Browser verification was carried forward from the
[P07/P08 frontend report](2026-11-13_p07_p08_frontend.md). The browser suite was
last run before consolidation, and its report is in the recovery ZIP.
