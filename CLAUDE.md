# BAiTI — Project Agent Rules

> LEVEL 2 under the root Development SOP (`../../CLAUDE.md`). Extends it; never contradicts it.
> **Project**: BAiTI (formerly MyComunity) — multi-tenant community platform; first pilot is a landed RA.
> **State (3 November 2026)**: the published authentication stack passed 276 native tests today. Five stacked draft PRs preserve both original Git histories; main is not merged. RBAC, onboarding and full G-RLS remain unfinished. Resume from `docs/ai-context/01_CURRENT_STATUS.md` and `docs/testing/NATIVE_POSTGRES.md`. Proceed with P05 RBAC and actor auditing; features remain gated by complete I1–I9.

## Source of truth, in order

1. `docs/blueprint/` — the engineering blueprint. **Normative**: `03_ARCHITECTURE.md` §6–7 (tenancy/auth), `04_DATABASE_DESIGN.md` (schema/DDL), `08_ai_packets/` (work orders + executor protocol).
2. `docs/ai-context/` — session state; follow its README resume protocol at session start.
3. Legacy (`archive/legacy-plans/MyComunity_PLAN.txt`, data-model PDF) — historical reference only; on any conflict the blueprint wins (`docs/blueprint/00_REPOSITORY_INVENTORY.md §2–3`).

## Stack

Spring Boot 4.1.2 / Java 17 source (Temurin 21 runtime here) / Maven 3.9 · native PostgreSQL 16 + RLS · Flyway · planned React 18 + TS + Vite · JUnit Jupiter · planned Vitest. Docker/Testcontainers execution and hosting/VPS are deferred by the owner (27 October 2026).


## Non-negotiables

- **Tenant isolation before features**: no feature module until gate G-RLS (isolation suite I-1…I-9 green — `docs/blueprint/06_TESTING_STRATEGY.md §5`).
- **Dual DB roles**: Flyway runs as `baiti_migrator` (owner); the app runs as `baiti_app` (NOBYPASSRLS). Never merge them. FORCE RLS binds ordinary owners too; the privileged platform path requires an explicit tested policy design.
- **TDD**: failing test first, always. Tenancy/security code doubly so.
- **Schema**: only from `04_DATABASE_DESIGN.md`; never from the PDF; deviation register (04 §9) is law. Never edit an applied Flyway migration.
- **Money**: NUMERIC/BigDecimal end-to-end; floats are a defect.
- **No secrets in the repo**: env vars only; `.env` is gitignored; `.env.example` documents the contract.
- **Cross-tenant responses are 404**, never 403.
- **Evidence before claims**: command output captured for every "done" (verification-before-completion).

## Commit & git workflow (approval required)

1. Stage specific paths (never `git add -A`).
2. Draft the message → present to Faisal → he edits/approves → then commit.
3. Message format: [shared workspace commit SOP](../../standards/COMMIT_MESSAGES.md). Use its shared template and truthful review status; the approval step above still applies.
4. **Never push** without an explicit "push" instruction. The owner authorised publication on 3 November 2026. Public repository `github.com/FaisalHanafi98/BAiTI` is configured as origin; draft PRs #1–#5 preserve both original histories. The publication record is in `docs/ai-context/02_SESSION_HISTORY/2026-11-03_github_publication.md`.
5. No direct commits to `main`/`dev` once branches/protection exist; feature branches + PRs.

## Allowed commands

`./mvnw verify | test | spring-boot:run` (DB-dependent execution only against an established owned native target; use the native runner guide) · `npm ci | run dev | test | run build | run lint` (when frontend source exists) · `git status | diff | log | add <paths> | commit` (under the owner's current local-commit direction below) · `gh` read operations. Docker/Compose/WSL and hosted deployment work are deferred; preserved container commands are not current next steps.

## Forbidden

Unauthorised pushes; force-push; hard reset; editing applied migrations; hand-editing `frontend/src/api/types.gen.ts`; committing `.env`/secrets/real personal data (PDPA); `--dangerously-skip-permissions`; weakening isolation/authz tests to pass a build; creating artifacts under the old "MyComunity" name.

## Session duties

At session end (per `docs/ai-context/README.md`): update `docs/ai-context/01_CURRENT_STATUS.md`, add a `02_SESSION_HISTORY/` entry, file test evidence under `06_TESTING_EVIDENCE/` when tests ran.
## Native execution amendment — 29 October 2026

Direct owner direction supersedes Docker-only development prerequisites. Use
native PostgreSQL 16 and the explicit target/role preflight in
`docs/blueprint/06_TESTING_STRATEGY.md` section 3.6. The native harness and local
runner are implemented; follow `docs/testing/NATIVE_POSTGRES.md`. Docker,
Testcontainers, packaging, hosting/VPS and CD remain deferred to later P18.
Foundation tests use a labelled test-only tenant fixture; they do not implement
production P05 tenancy or close G-RLS. Full local I1–I9 still precedes features,
and activated GitHub CI repeats those gates before merge. This amendment does
not waive concrete commit-message approval or explicit push authorisation.

**Owner commit direction — 29 October 2026:** for this continuing BAiTI work,
the owner now authorises local commits after drafting each message with the
shared SOP, applying Humanizer and completing the relevant checks. A separate
message approval pause is no longer required. Group related commits into
meaningful PRs with their necessary tests and documentation. Explicit push
authorisation is still required.
