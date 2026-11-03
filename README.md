# BAiTI

A neighbourhood and resident commercial management system.

Multi-tenant community management platform. The intended isolation design uses JWT tenant claims, application tenant filtering and PostgreSQL row-level security; runtime isolation is not yet proved. The first pilot is a landed RA, combining RA management with later neighborhood commerce.

**Blueprint**: The complete engineering blueprint lives at [`docs/blueprint/`](docs/blueprint/README.md) — start there. Implementation proceeds by executing the AI work packets in [`docs/blueprint/08_ai_packets/`](docs/blueprint/08_ai_packets/README.md).

**Status — 29 October 2026**: P03/P04 persistence acceptance and P05 tenant-context wiring pass 208 native tests. Real authentication, complete G-RLS and the frontend remain unfinished. Docker/Testcontainers, hosting/VPS and deployment stay deferred. Start with [current evidence](docs/ai-context/01_CURRENT_STATUS.md), [native tests](docs/testing/NATIVE_POSTGRES.md), the [runtime decision](docs/adr/0002-supported-spring-baseline.md) and [platform policy](docs/adr/0003-explicit-platform-table-policies.md).

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

The ordinary development database is separate. Production tenant resolution and
onboarding remain unfinished. Docker/Testcontainers, packaging, hosting/VPS and
CD are deferred; existing infra files are preserved for that later stage.

## Ground rules

Agents and humans alike: read [`CLAUDE.md`](CLAUDE.md). Follow TDD, prove tenant isolation before feature work and keep secrets outside Git. The owner authorises continuing local commits after the shared SOP, Humanizer and relevant checks. **Never push without explicit instruction.**

## Collaborators

Faisal Hanafi (backend lead) · Anep (frontend lead) — decisions log: `meeting/05_OUTCOME.md`.

*Legacy note: this project was planned under the name "MyComunity"; `MyComunity_PLAN.txt` and the data-model PDF are kept for traceability but are superseded by `docs/blueprint/`.*
