# BAiTI — Current Status

## Verified native foundation — 29 October 2026

The complete native PostgreSQL 16.15 suite passes **57 unit/architecture +
45 integration tests**, with no failures, errors or skips. Source commits
`3c78d92`, `cb2399d`, `e2bb68c`, `aa126eb` and `2602fc4` cover the actor
correction, native harness, permission persistence, V29 and history protection.
See [the acceptance mapping](06_TESTING_EVIDENCE/2026-10-27_native_harness.md),
[structured audit](../audits/foundation-2026-10-29/VERIFICATION.json) and
[session record](02_SESSION_HISTORY/2026-10-29_foundation_checkpoint.md).

The owner authorises continuing local commits after the shared SOP, Humanizer
and relevant checks. Group related commits into meaningful PRs. No push or PR
has occurred; explicit publication authorisation remains required. Earlier
unrelated local research/file-organisation work is preserved.

P03/P04 acceptance still has explicit gaps. Test-only tenancy is not P05;
platform provisioning, complete constraint/enum coverage, all I1–I9, changed-line
coverage and hosted CI remain open. The next bounded step is the isolated
[supported-runtime upgrade](../adr/0002-supported-spring-baseline.md) before
production tenancy/authentication. Docker, hosting/VPS and deployment stay deferred.

The sections below are historical checkpoints and do not override this result.

> **Last Updated**: 2026-08-24 (P04 completed and committed at `9e6e22d`, `2b5cd7c` — compile & Spotless GREEN across 55 Java classes).
> **Updated By**: Antigravity AI (Reviewed manually by Faisal)
> **Evidence Basis**: Direct repository inspection, `cmd /c mvnw.cmd spotless:apply test-compile` (BUILD SUCCESS in 11.7s), and Git commit log.
> **Project name**: BAiTI (multi-tenant condominium management platform)

---

## Current Goal

Proceed to **Packet 05 (Security & Tenancy Wiring)**: implement JWT authentication filter, `TenantContext`, `@TenantId` multi-tenancy resolver, and Spring Security RBAC chain.

## What Changed 2026-08-23 (P03 Database Migrations V1–V28)

Authored all 28 Flyway SQL database migrations in `backend/src/main/resources/db/migration/` covering all 26 relational entities, tenant row-level security (RLS) enforcement, and append-only grants (commits `4e7e257`, `807e560`, `f82927c`).

## What Changed 2026-08-24 (P04 Core Domain Entities & Repositories)

Mapped all 26 relational tables to JPA entities extending `TenantAwareBaseEntity` with Hibernate 6 `@TenantId` and Spring Data JPA repositories across all 11 domain packages. Fixed `mvnw.cmd` single-quote path handling. Verified with `mvn spotless:apply test-compile` (BUILD SUCCESS, 55 classes) (commits `9e6e22d`, `2b5cd7c`).

## What Changed This Session (2026-07-07)

**The complete engineering blueprint now exists at `docs/blueprint/`** — nine documents + 19 AI implementation packets. It supersedes `MyComunity_PLAN.txt` and all older planning facts where they conflict (`docs/blueprint/00_REPOSITORY_INVENTORY.md §2`). Discovery corrected several defects in the old plan:

| Finding | Where fixed |
|---|---|
| PDF holds **23** tables, not 22; final schema = **26 relations** | blueprint 00 §3.1, 04 §2 |
| 8 additional PDF typos (incl. `premission_id`, `submitted at`, `update_at`, singular `block`/`notification`) | 04 §9 deviation register (complete list) |
| Migration ordering defect (`refresh_tokens` after RLS; numbering conflict §5.4 vs §8) | corrected sequence V1–V28, 04 §6 |
| **RLS would not have bound the table owner** — no migrator/app role split existed in any doc | 03 §6.2 dual roles + FORCE RLS + CI canary |
| `SET LOCAL` interceptor design was a no-op outside transactions | 03 §6.3 pool-safe `TenantAwareDataSource` |
| `payments.payment_method` free text vs `payment_methods` table | 04 §4.5 FK normalization |
| Missing SRS / use cases / access matrix / threat model / UAT / DR plans (2026-05-19 audit gaps) | blueprint docs 01, 02, 05, 06 |

## What Changed This Session (2026-07-08, session 2 — P02 authoring)

P02 (backend skeleton) was **authored and compile-verified**, but not yet committed (pending Faisal's
approval) and not yet runtime-verified (Docker daemon unavailable). Full detail in
`docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md`.

| Item | Status |
|---|---|
| `backend/` Maven project (Spring Boot 3.3.5 / Java 17) + wrapper (Maven 3.9.9) | CREATED |
| `DataSourceConfig` dual-role wiring (`baiti_app` pool + Flyway `baiti_migrator` via props) | CREATED |
| `SecurityConfig` (`/actuator/health` permitAll until P05) + `StartupDiagnostics` (logs current_user) | CREATED |
| `application.yml` + dev/test/prod profiles (ddl-auto validate, prod no secret defaults) | CREATED |
| `HealthIT` (singleton PG16 Testcontainer, dual-role, health UP) | CREATED (not executed) |
| `mvn test-compile` = BUILD SUCCESS; Spotless clean; 5 classes compiled | **VERIFIED** |
| `./mvnw verify` (HealthIT) + `spring-boot:run` boot proof + compose role checks | **UNVERIFIED — Docker-gated** |

## What Changed 2026-07-25 (frontend strategy session — paper only)

`docs/plans/FRONTEND_FOUNDATION_v1.md` (DRAFT) now exists: IA (one SPA, three
role-shaped surfaces), five journey wireflows, design-system/token + tenant
theming architecture, RN-portability guardrails, phased roadmap gated behind
G-RLS + Anep ratification, and a sign-off register (D-A1…A7 for Anep;
D-F1…F3 for Faisal — note **D-F2: BigDecimal must serialise as JSON string
before the first frozen OpenAPI contract**). No code, no commits. Details:
`02_SESSION_HISTORY/2026-07-25_frontend_foundation_strategy.md`.

## Current Blockers (updated 2026-07-08, session 2)

| Blocker | Gate | Status |
|---|---|---|
| Dev environment | P00 | **DONE with one deferral** — Java 21 ✓, Maven 3.9.16 installed ✓, Node 24 ✓, git ✓, gh (FaisalHanafi98) ✓; **Docker daemon not starting — deferred by user (installation problem)**; required again now for P02 DoD (Testcontainers) and P01 residue (compose role checks). Evidence: `docs/audits/PHASE0_VERIFICATION.md` |
| P02 DoD (HealthIT + boot proof) | P02 | **BLOCKED on Docker** — `./mvnw verify` and `spring-boot:run` need the daemon; compile + Spotless already GREEN |
| Folder rename → `baiti` | G-NAME | **APPROVED**; physical rename blocked by the running session's file lock — run after closing the session: `Rename-Item "...\Anep's Project (Baiti)" -NewName "baiti"` |
| Anep alignment | G-ANEP | **CLEARED** — `meeting/05_OUTCOME.md` (user confirmation) |
| GitHub repo location | G-PUSH | **DECIDED** — `FaisalHanafi98/baiti` private; actual push still needs explicit instruction |
| Deployment target | D3 | **DECIDED** — Fly.io SG (used at P18) |

## Current Code / Database / Testing State

**P02 backend skeleton exists** (untracked, uncommitted): `backend/` Maven project with 4 main
classes + `HealthIT`; `mvn test-compile` = BUILD SUCCESS; Spotless clean. **No runtime verification
yet** — `HealthIT` not executed (Docker down). **No database** has ever run (Docker never came up).
`infra/docker-compose.yml` + `infra/db-init/01_roles.sh` still unverified. No migrations yet (P03).

## SDLC Phase Status (blueprint milestones)

| Milestone | Status |
|---|---|
| Blueprint (this package) | **DONE 2026-07-07** |
| M0 environment (P00) | **DONE** (Docker deferred) |
| M1 foundation (P01–P03) | **IN PROGRESS** — P01 done (commit `467d574`); P02 authored (compile-verified, Docker-gated DoD open, uncommitted); P03 not started |
| M2 security core (P04–P05, G-RLS) | NOT STARTED |
| M3 rails (P06–P07) | NOT STARTED |
| M4 v1.0 modules (P08–P12) | NOT STARTED |
| M5 hardening · M6 go-live · M7 v1.x | NOT STARTED |

## Next 5 Recommended Actions

1. **User**: resolve the Docker daemon (installation problem) — it unblocks the P02 DoD and the P01 compose role checks.
2. **Agent**: once Docker is up, run U1–U4 (`docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md §5`): `./mvnw verify` (HealthIT) → `spring-boot:run` boot proof (current_user=baiti_app) → compose role checks; append results to the evidence file.
3. **Faisal**: review the P02 files + evidence; approve the commit message; then commit (per CREAMS SOP — no auto-commit).
4. Execute **P03** — all 28 migrations V1–V28 with `MigrationIT` (closes the `fail-on-missing-locations` relaxation).
5. Execute **P04–P05** — entities + the security/tenancy core, exiting at **G-RLS** with the isolation suite green.

## Do-Not-Repeat List (updated)

Everything from the previous list still applies (no PDF typos, no `git add -A`, no pushes, no auto-commits, no editing applied migrations, TDD always, no code in P00/P01) **plus**:
- Do NOT treat `MyComunity_PLAN.txt` migration numbering as current — `docs/blueprint/04_DATABASE_DESIGN.md §6` is the only sequence.
- Do NOT run the app and Flyway as the same DB role — dual-role rule (blueprint 03 §6.2).
- Do NOT start any feature module before gate G-RLS evidence exists.
- Do NOT create new artifacts under the MyComunity name.
