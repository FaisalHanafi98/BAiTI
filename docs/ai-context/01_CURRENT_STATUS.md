# BAiTI — Current Status

## Permissions foundation verified — 4 November 2026

Continuing implementation is authorised by the owner after the planning intake.
Branch `igumi`, based on `4ce314b`, in `backend/.local/github-publication`. Full
native verification passed **150 unit/architecture + 152 integration tests**,
zero failures/errors/skips; Maven reported 1 minute 6 seconds. See
[evidence](06_TESTING_EVIDENCE/2026-11-04_permissions.md) and the
[session record](02_SESSION_HISTORY/2026-11-04_p05_permissions.md).

The 42-code tenant catalogue and four default-role maps match the access-grid
fixture. Per-tenant sync uses the ordinary app role and a transaction-level
advisory lock, creates missing rows and preserves existing-code revocations and
custom roles. Startup discovers tenant IDs through the bounded owner directory,
then binds/clears each scoped app transaction. Test bootstrap disables automatic
sync and exercises it explicitly with rollback or fixture-owned UUID cleanup.
The seven environment keys and V1–V31 are unchanged.

Method security now denies an Admin principal without the named authority. The
HTTP matrix exercises 168 allow/deny combinations on a labelled test-only probe;
application mapping/OpenAPI enumeration requires every actual endpoint to be
classified. Future business endpoints still need their own permission/own-scope
tests. JpaConfig now supplies the authenticated matching-tenant user to actor
auditing; native persistence retains the creator and records a different editor.
Activity-log events and async propagation are not implemented by those stamps.

The earlier sandbox status reported no PG process and a bounded start attempt
timed out. Elevated status found the existing owned cluster at PID 36176; native
tests reused it. No database reset, role change, migration edit or successful
new cluster start is claimed. No app/production DB was used.

Next: resident ownership predicates/404 handling, then the separate super-admin
plane, atomic onboarding, auth audit/async propagation and complete I1–I9. Full
P05/G-RLS, dev browser/curl journeys, changed-line coverage and hosted/release
gates remain open. Local SOP/Humanizer commits are authorised; this continuation
does not authorise a new push. Keep only main/igumi/anep. Docker/hosting remain
deferred. Earlier sections are dated checkpoints.

## Workflow intake and P05c acceptance map — 4 November 2026

Planning checkpoint on `igumi`, HEAD `4ce314b`; the worktree was clean at intake.
The three local refs main/igumi/anep are aligned; live GitHub was not queried.
The new project workflow is mapped into the existing
[P05c work order](../blueprint/08_ai_packets/PACKET_05_security_tenancy.md#planning-baseline-and-acceptance-map--4-november-2026),
with source-present, missing and unresolved requirements distinguished. See the
[session record](02_SESSION_HISTORY/2026-11-04_workflow_baseline.md).

Next implementation is the permission catalogue/default-role contract fixture,
then scoped synchronisation, method/own-scope permissions and actor auditing.
Startup sync must preserve Admin-edited grants; the SRS seeds defaults at tenant
creation. Current actor auditing still returns an empty AuditorAware, and no
catalogue/sync/method-security implementation was found in the inspected source.
Platform onboarding, async propagation and complete I1–I9/G-RLS remain open.

The private PG16 server is stopped. The existing settings path and runner are
present; credentials, DB target ownership and role flags were not read/queried.
Java 21.0.11 and Maven 3.9.16 are available. No test run, DB connection, restart,
schema execution or application change occurred. The 276-test result remains
dated 3 November. No frontend directory exists in this checkout.

BT-M01 procurement, BT-A1 document retrieval and BT-A2 dues/reserve planning
remain proposed follow-ups after their actual prerequisites and RA contracts.
The new handoff does not replace the core pilot roadmap. Original-checkout
planning stays in place; its 84 local changes were left untouched. Today's
changes are uncommitted planning documentation only, with no new push or branch.
Docker, hosting/VPS and deployment remain deferred.

## Three-branch workflow — 3 November 2026

The owner replaced the five-branch review stack with exactly three branches:
`main` for production, `igumi` for Faisal and `anep` for the stakeholder. All three
share the current implementation as their starting point. Both original Git
histories and all 23 original local commits remain reachable. Draft PRs #1–#5
are closed; their branches are obsolete. See the
[owner-approved workflow](02_SESSION_HISTORY/2026-11-03_three_branch_workflow.md).

Continue in `backend/.local/github-publication` on `igumi`, tracking origin/igumi.
Use the appropriate author branch for future work and meaningful PRs to main;
do not create additional branches without owner direction. Initial alignment
does not authorise overwriting later work on either author's branch.

The original checkout and older test/runtime worktrees remain detached at their
existing commits. Their files are retained; the original checkout's 84 local
changes were verified unchanged during detachment. No reset or worktree removal
was used. Only obsolete branch references are removed.

This consolidation changes Git references and documentation. Backend, migration,
tool and infra files remain those tested earlier today: 276 tests, zero failures,
errors or skips. No test rerun is claimed for these documentation-only changes.
The production branch name does not close P05/G-RLS or establish deployment.
Next remains P05c RBAC/catalogue and actor auditing; Docker, hosting/VPS and
deployment stay deferred. Earlier sections are historical checkpoints.

## Published review stack — 3 November 2026

The owner explicitly requested pushing the existing commits. Origin now points
to `https://github.com/FaisalHanafi98/BAiTI.git`. All 23 original local commits
through `19a7dce` and the owner's initial `16d2621` remain reachable in the
published stack. No history was rewritten and no force-push occurred.

Continue in `backend/.local/github-publication` under the original project,
branch `publish/jwt-authentication`, tracking the same branch on origin. This
registered worktree supersedes `backend/.local/persistence-acceptance` as the
continuation checkout. Earlier worktrees and the original checkout's uncommitted
research/file-organisation changes are preserved.

Five draft PRs cover foundation → runtime → persistence → tenant context →
authentication. Their bases form a stack; review and merge in that order with
merge commits. Main still contains the owner's initial commit until those PRs
are merged. See the [publication record](02_SESSION_HISTORY/2026-11-03_github_publication.md).

Today's full native run passed **140 unit/architecture + 136 integration tests**,
zero failures/errors/skips; Maven reported 1 minute 23 seconds. The backend,
migration, tool and infra trees match `19a7dce`. The existing private PG16 cluster
was restarted without resetting databases or altering roles. See
[verification evidence](06_TESTING_EVIDENCE/2026-11-03_publication.md).

Next remains P05c RBAC/catalogue and actor auditing, then super-admin onboarding,
auth audit/async propagation and the complete I1–I9 gate. Draft publication does
not close G-RLS, changed-line coverage, browser journeys, hosted CI or release
readiness. Docker, hosting/VPS and deployment stay deferred. The entries below
are dated checkpoints and do not override this publication state.

## P05 authentication workflows verified — 30 October 2026

Branch `feat/jwt-authentication`, following access-token commit `a3f1e68`, in
`backend/.local/persistence-acceptance` under the original checkout. Full native
verification passes **140 unit/architecture + 136 integration tests**, zero
failures/errors/skips, in 51.630 seconds. See
[workflow evidence](06_TESTING_EVIDENCE/2026-10-30_authentication_workflows.md) and
[structured record](../audits/jwt-authentication-2026-10-29/AUTH_FLOW.json).

POST login/refresh/logout and GET me now use real native DB users, bcrypt and the
signed tenant identity. Refresh tokens are 256-bit opaque values with SHA-256
hashes stored in PostgreSQL. Rotation locks the row; replay revocation commits
before 401, including the concurrent replay case. Logout revokes every refresh
family for that user. Access tokens retain their signed expiry; logout does not
add a bearer-token denylist. Only login/refresh are anonymous routes.

Cookie refresh requires an exact allowed Origin and X-BAiTI-Refresh: true. The
cookie is Secure/HttpOnly/SameSite=Strict, host-only, path /api/auth/refresh.
Body fallback accepts an explicit token without an ambient cookie; replacement
is returned in Set-Cookie. CORS uses an explicit credentialled allowlist. Login
limits apply per IP and per (tenant,email); refresh has a separate IP budget.
The local cache is bounded, and auth bodies are capped at 8 KB before parsing.
Application auth errors now use the blueprint's RFC 7807 contract without echoing
credential values. Token TTL configuration supports shorter whole-second values
within the published 15-minute access / 7-day refresh limits.

Next: P05 5c RBAC/catalogue and actor auditing, followed by super-admin onboarding,
auth audit/async propagation and complete I1–I9. This native MockMvc/DB evidence
does not close G-RLS or establish browser/curl dev journeys, changed-line coverage,
hosted CI, production key operations or release readiness. Docker/hosting stay
deferred. An overnight native-server outage was diagnosed at preflight; the
existing owned cluster was restarted without resetting data, then reruns passed.

GitHub was checked read-only on 30 October: it still contains only the owner's
initial commit 16d2621. Local commits are not published; no remote is configured.
Explicit push authorisation remains required. Earlier sections are dated checkpoints.

## P05 access-token boundary verified — 29 October 2026

Active branch `feat/jwt-authentication`, based on tenant-context commit `91d6f61`,
in the existing `backend/.local/persistence-acceptance` worktree. Full native
verification passes **119 unit/architecture + 119 integration tests**, with zero
failures/errors/skips. See [access-token evidence](06_TESTING_EVIDENCE/2026-10-29_jwt_access.md)
and [structured record](../audits/jwt-authentication-2026-10-29/VERIFICATION.json).

HS256 access tokens carry tenant/user, roles/permissions, issuer, audience, token
purpose and jti. Required claims, algorithm and signature are checked; expiration
is strict at the 15-minute boundary. The stateless HTTP chain verifies bearer
identity against the resolved community and denies tenant tokens on platform
routes. Default generated-user/form/basic/session fallback is disabled. Main
startup now requires a Base64 JWT_SECRET containing at least 32 decoded bytes;
native tests generate a separate per-process in-memory test key.

This is the access-token portion of 5b. No password-login, refresh, logout or me
endpoint is implemented yet; tests issue synthetic signed principals directly.
Next is the real tenant-aware login/refresh flow, rate limits and cookie/CSRF
contract, followed by RBAC, actor auditing, onboarding, async propagation and
full I1–I9. Full G-RLS, changed-line coverage and hosted CI remain open. No feature
module, push, PR, Docker or hosting change. Local SOP/Humanizer commits continue.

## P05 slice 5a verified — 29 October 2026

Active worktree: `backend/.local/persistence-acceptance` in the original checkout,
branch `feat/tenant-context`, based on persistence commit `c4ffbac`. Native PG16
verification passes **95 unit/architecture + 113 integration tests** with zero
failures/errors/skips. See [slice evidence](06_TESTING_EVIDENCE/2026-10-29_tenant_context.md)
and [structured audit](../audits/tenant-context-2026-10-29/VERIFICATION.json).

Production TenantContext now feeds the primary app datasource and Hibernate.
A one-connection pool proves A/B binding and cleanup, including unfinished and
aborted transactions. Binding/reset/rollback failures are tested to evict the
connection. Header resolution (dev/test), configured subdomains (prod) and
request-finally cleanup are implemented. Global slug lookup uses an explicit
annotated owner-role pool; ordinary JPA/JDBC remain on baiti_app. No super-admin
HTTP access or onboarding is inferred from that lookup seam.

Next: P05 slice 5b, real JWT authentication and rotating refresh tokens. The
current security chain still requires authenticated users but uses the skeleton
baseline; mock-user checks are not real-login evidence. JWT tenant matching,
RBAC, actor auditing, I8/I9 and complete G-RLS remain open, as do changed-line
coverage and hosted CI. Do not start feature modules. Docker/hosting remain
deferred. Local SOP/Humanizer commits are authorised; push is not.

## Persistence acceptance verified — 29 October 2026

P03/P04 local persistence acceptance now passes **59 unit/architecture + 100
integration tests**, with zero failures, errors or skips, on both a V30→V31
upgrade and a separate fresh V1–V31 database. V31 adds the six missing FK indexes
identified by the failing catalog regression. V1–V30 bytes remain unchanged.
See [the acceptance record](../audits/persistence-acceptance-2026-10-29/P04_ACCEPTANCE.json)
and [acceptance mapping](06_TESTING_EVIDENCE/2026-10-29_persistence_acceptance.md).

All 15 authored business unique indexes, the role-grant composite key, all 16
entity enum fields and the lazy-association contract have explicit checks.
Tenant cases use baiti_app; global JPA cases use a labelled test-only owner pool.
Timestamp auditing is exercised, while actor auditing remains the explicit P05
stub. Constraint tests do not establish service transition permissions.

Next: P05 slice 5a, production tenant propagation and pool return cleanup, followed
by authentication, RBAC, platform onboarding and complete I1–I9. G-RLS,
changed-line coverage, hosted CI and deployment remain open. Docker/hosting stay
deferred. Local commits remain authorised after SOP/Humanizer and checks;
publishing is not authorised. Earlier sections are dated checkpoints.

## Platform SQL boundary verified — 29 October 2026

Active branch `test/persistence-acceptance`, based on runtime commit `781a401`.
V30 implements the bounded global owner policies from
[ADR 0003](../adr/0003-explicit-platform-table-policies.md). The full suite passes
**58 unit/architecture and 53 integration tests**, with no failures/errors/skips,
on both a V29→V30 fixture and a separate empty V1–V30 target. All prior SQL hashes
are unchanged. See [policy evidence](../audits/persistence-acceptance-2026-10-29/VERIFICATION.json)
and [session history](02_SESSION_HISTORY/2026-10-29_platform_policy.md).

The migrator can select/insert/update global tenants/admins without becoming
superuser or BYPASSRLS; DELETE remains denied. Existing-row canaries prove app
global denial and role-escalation denial. Domain RLS still binds both roles.
The test fixtures are synthetic and explicitly provisioned through the intended
owner. This SQL path does not implement P05's HTTP caller/privileged-pool boundary
or its complete onboarding transaction.

Next: finish remaining P03/P04 uniqueness/enum and privileged entity-mapping
acceptance before production tenancy/authentication. Full G-RLS, changed-line
coverage, hosted CI and release remain open. Continuing local commits follow
SOP/Humanizer and checks; no push or PR is authorised or performed.

Earlier runtime/foundation sections below are dated checkpoints.

## Supported runtime verified — 29 October 2026

Branch `chore/supported-spring-baseline` is an isolated local worktree based on
foundation commit `1aec31c`. Boot 4.1.2 and springdoc 3.2.0 pass **58 unit/architecture
and 47 integration tests**, with zero failures/errors/skips. The old runtime guard
failed on Boot 3.3.5; the documented 3.5 bridge compiled before the 4.x adaptation.
A new empty native PG16 database also applied all 29 existing migrations; its
subsequent full fixture run passed the same 105 tests in 41.635 seconds.
See [ADR 0002](../adr/0002-supported-spring-baseline.md),
[runtime evidence](../audits/runtime-upgrade-2026-10-29/VERIFICATION.json) and
[the session record](02_SESSION_HISTORY/2026-10-29_supported_runtime_upgrade.md).

All SQL bytes remain unchanged. The inherited test-only tenant fixture does not
implement production tenancy. Remaining P03/P04 constraint/enum cases, the explicit
platform policy, P05, I1–I9, changed-line coverage, hosted CI and release remain open.
The foundation branch and unrelated research/files are preserved. No push or PR
has occurred. The owner authorises continuing local commits after SOP/Humanizer
and checks; keep the runtime upgrade as its own meaningful PR after foundation.

The earlier foundation checkpoint below is retained as dated baseline evidence.

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
