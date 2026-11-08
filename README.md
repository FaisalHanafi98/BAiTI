# BAiTI

A neighbourhood and resident commercial management system.

Multi-tenant community management platform. Tenant isolation uses signed tenant claims, scoped application queries and PostgreSQL row-level security. The first pilot is a landed residents' association, combining RA management with later neighbourhood commerce.

**Blueprint**: The complete engineering blueprint lives at [`docs/blueprint/`](docs/blueprint/README.md) — start there. Implementation proceeds by executing the AI work packets in [`docs/blueprint/08_ai_packets/`](docs/blueprint/08_ai_packets/README.md).

**Status — 8 November 2026.** P05 and local G-RLS passed on PostgreSQL 16 with 321 tests. The dev profile also passed the two-community authentication smoke. Work continues on `igumi` under the [branch workflow](docs/ai-context/02_SESSION_HISTORY/2026-11-03_three_branch_workflow.md). GitHub CI, Docker, hosting and deployment have separate gates. Start with the [current evidence](docs/ai-context/01_CURRENT_STATUS.md) and the [native test guide](docs/testing/NATIVE_POSTGRES.md).

## Stack

Spring Boot 4.1.2 · Java 17 source / Java 21 runtime · Maven · native PostgreSQL 16 (+RLS) · Flyway · JUnit Jupiter. React 18, TypeScript, Vite, Tailwind, TanStack Query and Vitest remain the planned frontend stack. Docker/Testcontainers parity and hosting are deferred; GitHub CI is not activated.

## Layout

```
backend/    Spring Boot (package root my.baiti)      — authored foundation
frontend/   Vite + React + TS                         — planned, absent
infra/      preserved container configuration, deferred
docs/       blueprint/ (source of truth) · adr/ · ai-context/ · audits/ · …
meeting/    collaboration records
```

## Native foundation checks

Use the [native run guide](docs/testing/NATIVE_POSTGRES.md) to establish a separate
owned PostgreSQL 16 test target, private settings and synthetic fixtures. Then run:

```powershell
./tools/testing/Invoke-NativeTests.ps1
```

Use a separate PostgreSQL database for normal development. The
[dev seed guide](docs/ai-context/04_DATABASE_STATE/seeders_status.md) describes
the opt-in fictional communities and their first-use credentials.

## Ground rules

Agents and humans alike: read [`CLAUDE.md`](CLAUDE.md). Follow TDD, prove tenant isolation before feature work and keep secrets outside Git. The owner authorises continuing local commits after the shared SOP, Humanizer and relevant checks. **Never push without explicit instruction.**

## Collaborators

Faisal Hanafi (backend lead) · Anep (frontend lead) — decisions log: `meeting/05_OUTCOME.md`.

*Legacy note: this project was planned under the name "MyComunity"; `MyComunity_PLAN.txt` and the data-model PDF are kept for traceability but are superseded by `docs/blueprint/`.*
