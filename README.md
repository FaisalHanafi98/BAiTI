# BAiTI

A neighbourhood and resident commercial management system.

Multi-tenant community management platform. Tenant isolation uses signed tenant claims, scoped application queries and PostgreSQL row-level security. The first pilot is a landed residents' association, combining RA management with later neighbourhood commerce.

**Blueprint**: The complete engineering blueprint lives at [`docs/blueprint/`](docs/blueprint/README.md) — start there. Implementation proceeds by executing the AI work packets in [`docs/blueprint/08_ai_packets/`](docs/blueprint/08_ai_packets/README.md).

**Status — 17 November 2026.** The single BAiTI checkout uses `igumi` for development. The local P07 authentication shell and P08 property workflows have passed native PostgreSQL and browser checks. P09 People is next. See the [current status](docs/ai-context/01_CURRENT_STATUS.md) and [native test guide](docs/testing/NATIVE_POSTGRES.md).

## Stack

Spring Boot 4.1.2 · Java 17 source / Java 21 runtime · Maven · native PostgreSQL 16 (+RLS) · Flyway · JUnit Jupiter. React 18, TypeScript, Vite, Tailwind, TanStack Query and Vitest power the frontend. Docker/Testcontainers parity and hosting are deferred; GitHub CI is not activated.

## Layout

```
backend/    Spring Boot (package root my.baiti)      — authored foundation
frontend/   Vite + React + TS                       — auth and property
infra/      preserved container configuration, deferred
docs/       blueprint/ (source of truth) · adr/ · ai-context/ · audits/ · …
meeting/    collaboration records
```

See the [project overview](docs/PROJECT_OVERVIEW.md) and [documentation index](docs/README.md) for the roadmap and planning material.

## Native foundation checks

Use the [native run guide](docs/testing/NATIVE_POSTGRES.md) to establish a separate
owned PostgreSQL 16 test target, private settings and synthetic fixtures. Then run:

```powershell
./tools/testing/Invoke-NativeTests.ps1
```

Use a separate PostgreSQL database for normal development. The
[dev seed guide](docs/ai-context/04_DATABASE_STATE/seeders_status.md) describes
the opt-in fictional communities and their first-use credentials.

## Frontend development

Start the backend against an owned dev database using the dev seed guide above.
Then run these commands from the frontend directory.

```powershell
npm ci
npm run dev
```

Open `http://localhost:5173` and sign in with a seeded community code and
credentials. Vite proxies `/api` to `http://localhost:8080` by default.
Set `BAITI_DEV_API` before starting Vite if the local backend uses another
port. Set the backend CORS allowlist to the actual browser origin.

Run `npm run lint`, `npm run typecheck`, `npm test` and `npm run build` from
the frontend directory. `npm run generate:api` regenerates client types from
`backend/openapi.json`, a snapshot from authenticated `GET /v3/api-docs`
with the request-specific servers entry omitted.

The Chrome acceptance test uses a dedicated dev backend with a short access
token lifetime and two fictional communities. Supply the first community's
temporary password as `BAITI_BROWSER_INITIAL_PASSWORD`, its new password as
`BAITI_BROWSER_REPLACEMENT`, and the second community's current password as
`BAITI_BROWSER_OTHER_PASSWORD`. Set `BAITI_DEV_API` to that backend URL and
run `npm run test:browser`. Use a first-use fixture for each run because the
test changes its password. Keep the fixture credentials outside Git.

## Ground rules

Agents and humans alike: read [`CLAUDE.md`](CLAUDE.md). Follow TDD, prove tenant isolation before feature work and keep secrets outside Git. The owner authorises continuing local commits after the shared SOP, Humanizer and relevant checks. **Never push without explicit instruction.**

## Collaborators

Faisal Hanafi (backend lead) · Anep (frontend lead) — decisions log: `meeting/05_OUTCOME.md`.

*Legacy note: this project was planned under the name "MyComunity"; `MyComunity_PLAN.txt` and the data-model PDF are kept for traceability but are superseded by `docs/blueprint/`.*
