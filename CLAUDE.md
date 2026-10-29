# BAiTI — Project Agent Rules

> LEVEL 2 under the root Development SOP (`../CLAUDE.md`). Extends it; never contradicts it.
> **Project**: BAiTI (formerly MyComunity) — multi-tenant SaaS condominium management.
> **State**: pre-code, bootstrap phase. Next packet: **P02** (backend skeleton).

## Source of truth, in order

1. `docs/blueprint/` — the engineering blueprint. **Normative**: `03_ARCHITECTURE.md` §6–7 (tenancy/auth), `04_DATABASE_DESIGN.md` (schema/DDL), `08_ai_packets/` (work orders + executor protocol).
2. `docs/ai-context/` — session state; follow its README resume protocol at session start.
3. Legacy (`MyComunity_PLAN.txt`, data-model PDF) — historical reference only; on any conflict the blueprint wins (`docs/blueprint/00_REPOSITORY_INVENTORY.md §2–3`).

## Stack

Spring Boot 3 / Java 17+ (Temurin 21 on this machine) / Maven 3.9 · PostgreSQL 16 + RLS · Flyway · React 18 + TS + Vite · JUnit 5 + Testcontainers · Vitest.

## Non-negotiables

- **Tenant isolation before features**: no feature module until gate G-RLS (isolation suite I-1…I-9 green — `docs/blueprint/06_TESTING_STRATEGY.md §5`).
- **Dual DB roles**: Flyway runs as `baiti_migrator` (owner); the app runs as `baiti_app` (NOBYPASSRLS). Never merge them — RLS does not bind table owners.
- **TDD**: failing test first, always. Tenancy/security code doubly so.
- **Schema**: only from `04_DATABASE_DESIGN.md`; never from the PDF; deviation register (04 §9) is law. Never edit an applied Flyway migration.
- **Money**: NUMERIC/BigDecimal end-to-end; floats are a defect.
- **No secrets in the repo**: env vars only; `.env` is gitignored; `.env.example` documents the contract.
- **Cross-tenant responses are 404**, never 403.
- **Evidence before claims**: command output captured for every "done" (verification-before-completion).

## Commit & git workflow (overrides root SOP §13.3 — no auto-commits here)

1. Stage specific paths (never `git add -A`).
2. Draft the message → present to Faisal → he edits/approves → then commit.
3. **Messages follow the CREAMS commit SOP** (`../CREAMS/docs/COMMIT_MESSAGE_SOP.md`), adapted to `Project: BAiTI`. Template in `.gitmessage`. In short: `Type(Scope): Sentence-case title.` (< 72 chars, full stop) · body with `Project:`, `Date: DD Month YYYY`, `Task:`, plain-language what/why · a numbered `Verified that:` list of real checks · why it matters · footer `[Assisted by AI, reviewed manually by Faisal]` when AI contributed. UK English. **No Co-Authored-By trailers naming an AI model.**
4. **Never push** without an explicit "push" instruction. Repo remote (when created): `github.com/FaisalHanafi98/baiti`, private.
5. No direct commits to `main`/`dev` once branches/protection exist; feature branches + PRs.

## Allowed commands

`./mvnw verify | test | spring-boot:run` · `npm ci | run dev | test | run build | run lint` · `docker compose -f infra/docker-compose.yml up -d | down` · `git status | diff | log | add <paths> | commit` (post-approval) · `gh` read operations.

## Forbidden

Pushing; force-push; hard reset; editing applied migrations; hand-editing `frontend/src/api/types.gen.ts`; committing `.env`/secrets/real personal data (PDPA); `--dangerously-skip-permissions`; weakening isolation/authz tests to pass a build; creating artifacts under the old "MyComunity" name.

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
