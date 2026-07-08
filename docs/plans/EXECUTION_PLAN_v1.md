# BAiTI — Master Execution Plan v1

> **Document type**: Master implementation specification (execution plan export)
> **Produced**: 08 July 2026, at cross-model migration point
> **Audience**: any AI model or engineer continuing this project with ZERO access to prior conversations
> **Authority**: this plan and the committed blueprint (`docs/blueprint/`) are jointly authoritative. Where this plan cites a blueprint section, that section is the normative detail (full DDL, full permission grid, full test lists); this plan is the complete map of WHAT to execute, in WHAT order, under WHICH gates. Both are in the repository — nothing needed to continue exists outside it.
> **Module count**: the programme has exactly **19 implementation modules (packets P00–P18)** across **7 epics (E1–E7)** and **8 milestones (M0–M7)**. All 19 are documented below. Do not merge, skip, or reorder them except as the dependency rules allow.

---

## 1. Executive Overview

BAiTI (formerly MyComunity) is a multi-tenant SaaS condominium/community management platform. One deployment serves N communities ("tenants" = condos); each tenant's data is isolated at three independent layers (JWT claim → Hibernate `@TenantId` filter → PostgreSQL row-level security under a forced, non-owner runtime role). Product modules: tenancy, auth/RBAC, property, residents/family, finance, complaints, maintenance, facilities, community, visitors, notifications, audit.

**Current execution position (08 July 2026)**: P00 (environment) and P01 (repo bootstrap) are COMPLETE at commits `467d574` and `3525b39` on `main`. **Next module: P02 (backend skeleton)**, preceded by two recorded P01 leftovers (Docker daemon verification; folder rename — see §16 item 0 and `CURRENT_MODULE_STATE.md`). Zero application code exists yet; that is correct for this stage.

**How to continue**: read root `CLAUDE.md`, then `docs/ai-context/README.md` → `01_CURRENT_STATUS.md` → `CURRENT_MODULE_STATE.md` → `docs/blueprint/08_ai_packets/README.md` (binding executor protocol) → this plan §9 for order → the packet file for the module you are executing.

## 2. Engineering Objectives

1. **BR-1 Isolation as product**: N tenants per deployment, zero cross-tenant visibility, provable by permanent tests (the isolation suite is a release gate forever).
2. **BR-2 Money trail**: bills → payments → receipts with transactional integrity; `NUMERIC`/`BigDecimal` end-to-end; payments append-only.
3. **BR-3/BR-4 Resident self-service + staff RBAC**: complaints, maintenance, bookings, visitors behind granular method-level permissions.
4. **BR-5 Accountability**: append-only activity log for security-relevant actions.
5. **BR-6 Portfolio-grade discipline**: TDD mandatory, CI-gated coverage (backend ≥ 70 %, frontend ≥ 60 % changed lines), evidence before every done-claim.
6. **BR-7 PDPA**: no real personal data in repo/seeds/logs, ever.
7. **BR-8 Kept-open doors**: React Native client, payment gateway, BM language — enabled by design, not built in v1.0.

Full requirement catalogue with stable IDs (FR-…/NFR-…): `docs/blueprint/01_SRS.md`. Those IDs are referenced by packets and tests; never renumber.

## 3. Architectural Principles (non-negotiable)

1. **Modular monolith in a monorepo** — one Spring Boot deployable (`backend/`, package-by-feature under `my.baiti`), one React SPA (`frontend/`), boundaries enforced by ArchUnit tests, cross-package calls through service interfaces only.
2. **Shared-schema multi-tenancy**: every domain table carries `tenant_id UUID NOT NULL REFERENCES tenants(id)`; isolation via RLS policy `tenant_id = current_setting('app.current_tenant', true)::uuid` with `USING` and `WITH CHECK`, `ENABLE` + `FORCE ROW LEVEL SECURITY` on all 24 tenant tables.
3. **Dual database roles**: Flyway runs as `baiti_migrator` (schema owner); the application pool runs as `baiti_app` (`NOBYPASSRLS`). RLS does not bind table owners — collapsing these roles silently voids all isolation. Roles are created by infra bootstrap (`infra/db-init/01_roles.sh`), never by migrations.
4. **Fail-closed tenant propagation**: `TenantAwareDataSource` wraps Hikari — on borrow it runs `set_config('app.current_tenant', <uuid>, false)` or `RESET`; proxied `close()` always `RESET`s before pool return. Unset context ⇒ RLS predicate NULL ⇒ zero rows / rejected writes. (`SET LOCAL` is a no-op outside transactions — do not "simplify" to it.)
5. **Cross-tenant responses are 404, never 403** (no existence oracle).
6. **TDD always**; tenancy/security code doubly so. Applied Flyway migrations are immutable; fixes are new versions; forward-fix only, no undo migrations.
7. **Schema truth**: `docs/blueprint/04_DATABASE_DESIGN.md` only — never the legacy PDF (its 11-entry deviation register at §9 is law; the PDF contains typos and a miscount).
8. **Contract-first frontend**: OpenAPI generated from code; `frontend/src/api/types.gen.ts` is generated, never hand-edited.
9. **Time**: `TIMESTAMPTZ`/`Instant` UTC in storage and API; `Asia/Kuala_Lumpur` at display. **IDs**: UUIDv4. **Enums**: TEXT + CHECK constraints, `@Enumerated(STRING)`.
10. **No secrets in the repo**; env vars only; `.env.example` is the contract (inventory: `docs/blueprint/03_ARCHITECTURE.md §8`).

## 4. Overall Execution Strategy (greenfield — there is no refactoring programme)

This is a from-zero build, so the "refactoring strategy" slot is occupied by the greenfield execution strategy: (a) **security-core-first sequencing** — the tenancy/auth core (P03–P05) lands and is proven before any feature module, because retro-fitting isolation is the project's named worst-case; (b) **packet execution** — each module is a self-contained work order with its own DoD, executed by exactly one agent-session at a time, ending in evidence and user-approved commits; (c) **vertical slices with real infrastructure** — Testcontainers PostgreSQL for all integration tests (RLS does not exist on H2/mocks); (d) **backend-then-frontend inside each feature module**, frontend consuming the frozen OpenAPI contract; (e) **decided-but-reversible baselines** — ambiguous product questions carry a recorded default (§14 decisions) so work never blocks on a stakeholder answer.

## 5. Complete Implementation Roadmap (milestones and integration checkpoints)

```
M0 ──▶ M1 ──▶ M2 ══[G-RLS]══▶ M3 ──▶ M4 ──▶ M5 ──▶ M6 (v1.0 LIVE) ──▶ M7 (v1.x)
 P00    P01–P03   P04–P05      P06,P07  P08–P12  P13+hardening  P18      P14–P17
```

| Milestone | Packets | Exit criteria (all evidenced) | Integration checkpoint |
|---|---|---|---|
| **M0** Environment | P00 | Toolchain verified, output in `docs/audits/PHASE0_VERIFICATION.md` | — | **STATUS: DONE** (Docker deferred — see §16 item 0) |
| **M1** Foundation | P01, P02, P03 | Repo governed + committed; `./mvnw verify` green incl. health test; all 28 migrations apply on Testcontainers; migration test green; ArchUnit baseline green | App boots against compose DB as `baiti_app` | P01 DONE; P02, P03 pending |
| **M2** Security core | P04, P05 | Entities+repos with smoke tests; login/refresh/logout/me; onboarding atomic+idempotent; **isolation suite I-1…I-9 green = gate G-RLS** | curl transcript of full auth round-trip on dev profile, two seeded tenants |
| **M3** Rails | P06, P07 | CI green on test PR (both workflows, required checks); branch protection; FE login → protected dashboard against local BE; generated types wired | First full-stack round-trip through the real contract |
| **M4** v1.0 modules | P08, P09, P10, P11, P12 | Each module's DoD (§7); journeys J1–J3 pass on dev; isolation suite still green after every module | Journey demos per module; access-matrix fixture grows with each |
| **M5** Hardening | P13 + activities | k6 targets met (§12); security pass clean; UAT exit criteria met; observability drill ≤ 2 min | Perf baseline recorded in `docs/testing/PERF_BASELINE.md` |
| **M6** Go-live | P18 | Staging+prod on Fly.io SG; restore + rollback rehearsed; runbook exists; go-live checklist signed | Isolation canary green **on the production database** |
| **M7** v1.x | P14, P15, P16, P17 | Facilities/community/visitors/notification-centre live; journey J4 passes | Each lands as a normal module release through CI/CD |

Calendar (both developers ≈ 12 h/week combined, 30 % buffer included): M0–M2 ≈ 5–6 weeks · M3 ≈ 2 · M4 ≈ 8–10 · M5–M6 ≈ 3–4 → **v1.0 live ≈ 4.5–5.5 months from M1 start**; M7 adds ~6–8 weeks. (The legacy "3–4 months" claim is superseded — it ignored hardening and go-live.)

## 6. Epics

| Epic | Contains | Delivers | Status |
|---|---|---|---|
| **E1 Foundation** | P00–P03 | verified env · governed repo · runnable skeleton · migrated schema | P00, P01 done |
| **E2 Security core** | P04–P05 | entities/repos · tenancy+auth+RBAC · isolation proven (G-RLS) | not started |
| **E3 Delivery rails** | P06–P07 | CI/CD · frontend shell with real auth | not started |
| **E4 v1.0 modules** | P08–P12 | property · people · finance · complaints · maintenance | not started |
| **E5 Hardening** | P13 + M5 activities | dashboards · observability · perf/security/UAT passes | not started |
| **E6 Go-live** | P18 | staging · production · DR rehearsal · runbook | not started |
| **E7 v1.x modules** | P14–P17 | facilities · community · visitors · notification centre + jobs | not started |

## 7. Modules (all 19 — objectives, scope, dependencies, acceptance criteria, DoD)

Every module has a full work order at `docs/blueprint/08_ai_packets/PACKET_NN_*.md` — that file is the executable spec (files to create, architecture deltas, test lists). This section externalises the plan-level content for each. The **module pattern** shared by P08–P12/P14–P17 (backend slice → contract freeze → frontend slice; standard test set; standard DoD) is defined in `08_ai_packets/README.md §3` and applies wherever "pattern DoD" appears below.

**P00 — Environment verification** *(size S; deps none)* — Verify/install Java 17+, Maven 3.9+, Node 18+, Docker, git, gh; evidence to `docs/audits/PHASE0_VERIFICATION.md`. **DoD**: all seven commands succeed with minimum versions, outputs captured. **STATUS: DONE** — Java 21 Temurin, Maven 3.9.16 (manual install at `C:\Users\asbou\tools\apache-maven-3.9.16`; winget has no Apache.Maven), Node 24, git 2.54, gh authenticated as FaisalHanafi98; Docker Desktop 4.78 installed but daemon unresponsive — deferred, must be resolved before P02.

**P01 — Repo bootstrap & governance** *(S; deps P00; gate G-NAME)* — git init on `main`; `.gitignore`/`.gitattributes`/`.env.example`/`README`/`CLAUDE.md`/`AGENTS.md`/`.gitmessage`; `infra/docker-compose.yml` (postgres:16 + healthcheck + pgadmin profile) + `infra/db-init/01_roles.sh` (two-role bootstrap); ADR-0001 naming, ADR-0002 monolith; docs tree. **DoD**: ignore rules proven, roles verified NOBYPASSRLS-correct via compose, first user-approved commit, no remote. **STATUS: DONE** (commit `467d574`) **except** compose verification (Docker) and physical folder rename (session lock) — both recorded, close before P02.

**P02 — Backend skeleton** *(M; deps P01)* — Maven project `backend/`, package `my.baiti`, class `BaitiApplication`; deps: web, data-jpa, security, validation, flyway(+postgres plugin), actuator, postgres driver, lombok, springdoc, testcontainers, spring-security-test, archunit, micrometer-prometheus; plugins: surefire/failsafe split, Spotless(google-java-format), JaCoCo, OWASP dependency-check (CI profile); profiles dev/test/prod with **dual datasources** (Hikari←`DB_APP_*`, Flyway←`DB_MIGRATOR_*`), `ddl-auto: validate` everywhere, prod has no secret defaults; Maven wrapper committed. **Acceptance/DoD**: `./mvnw verify` green incl. `HealthIT` (singleton PG16 Testcontainer, health=UP); boot log proves pool user `baiti_app`; Spotless passes; evidence to `docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md`. Temporary allowance: `/actuator/health` anonymous until P05.

**P03 — Database migrations V1–V28** *(L; deps P02)* — Transcribe `04_DATABASE_DESIGN.md §4/§6` exactly: V1 citext + default privileges for `baiti_app`; V2 tenants; V3 super_admins; V4–V7 permissions/roles/roles_permissions/users; V8 refresh_tokens (**before RLS — the legacy plan had this after, a defect**); V9–V10 blocks/units; V11–V12 residents/family_members; V13–V15 payment_methods/bills/payments; V16–V18 complaint_categories/complaints/complaint_updates; V19 maintenance_requests; V20–V21 facilities/facility_bookings; V22–V23 announcements/events; V24 visitor_pre_registrations; V25 visitors; V26–V27 notifications/activity_logs; V28 enable_rls (ENABLE+FORCE+policy ×24, tenants self-row policy, super_admins no-policy, REVOKE UPDATE/DELETE on payments+activity_logs). No seed migrations exist by design (dev seeding is P05e code). **Acceptance/DoD**: `MigrationIT` green — clean apply on empty PG16 (roles pre-created in test init); 26 relations; `rowsecurity` and `forcerowsecurity` true ×24; policy present ×24; grant revocations verified; RLS raw-JDBC smoke (context A sees only A; unset sees zero; cross-tenant insert rejected); grep proves zero PDF-typo spellings; commits grouped per domain.

**P04 — JPA entities & repositories** *(L; deps P03)* — `TenantAwareBaseEntity` (`@MappedSuperclass`: UUID id, `@TenantId` tenantId, auditing Instants, createdBy/updatedBy UUIDs); all 26 relations mapped package-by-feature (`Tenant`/`SuperAdmin` do NOT extend the base); LAZY everywhere; STRING enums matching CHECK lists; repositories per aggregate root, package-private where possible; ArchUnit rules armed (entity inheritance, controller↛repository, cross-package via interfaces, JWT imports only in `security`, Instant-only temporals). **Acceptance/DoD**: `ddl-auto: validate` passes (mapping = schema); per-aggregate repository smoke tests (round-trip, unique violations, enum round-trip, auditing timestamps); ArchUnit green; `./mvnw verify` green with evidence.

**P05 — Security & tenancy core** *(XL — five mandatory sequential slices; deps P04; exit = gate G-RLS)*
- **5a Tenant context & fail-closed datasource**: `TenantContext` ThreadLocal + `TenantContextFilter` (clear in finally); `TenantResolver` profile pair (header `X-Tenant-Slug` dev / subdomain prod); `TenantAwareDataSource` set/reset; `@TenantId` resolver wired. Tests: fail-closed reads+writes, connection-bleed, owner-bypass canary (`current_user='baiti_app'`, `rolbypassrls=false`).
- **5b JWT auth**: `/api/auth/login|refresh|logout|me`; access JWT 15 min HS256 (claims userId, tenantId, roles, permissions, jti; secret env-only ≥256-bit); refresh = opaque 256-bit random, SHA-256-hashed in `refresh_tokens`, 7-day rotating, **family revocation on reuse**; web refresh via httpOnly Secure SameSite=Strict cookie scoped to the refresh path, body fallback for future mobile; bucket4j rate limiting per-IP and per-(tenant,email); generic 401s (wrong password / unknown user / unknown slug / suspended tenant all identical). Tests: full auth suite + JWT abuse set (expired, bad signature, alg:none, tampered claims, replayed rotated token).
- **5c RBAC**: `PermissionCatalog` enum = machine copy of the access matrix (`02_SYSTEM_ANALYSIS.md §8` — the complete role×permission grid); default role grants (Admin/Manager/Resident/Guard); `@PreAuthorize("hasAuthority(...)")` wiring; own-scope guard for `*_OWN` permissions; startup `PermissionSyncService` (idempotent per-tenant upsert when releases add permissions). Tests: parametrised authz-matrix harness + endpoint-enumeration assertion (unclassified endpoint fails the build).
- **5d Super-admin plane & onboarding**: separate `super_admins` principal + `/api/admin/**` filter chain + separate login; `TenantOnboardingService` — single transaction: tenant row, role/permission copies from catalog, first Admin user (`must_change_password=true`), audit; idempotent by slug (409, zero writes); runs on the privileged (migrator) datasource — the single annotated exception (`@UsesPrivilegedDataSource`); ArchUnit: `tenancy.admin` imports no domain repositories. Tests: rollback-on-failure, duplicate slug, tenant-admin cannot reach `/api/admin/**`.
- **5e Dev seed & the isolation suite**: `DevDataInitializer` (@Profile dev, idempotent, calls real onboarding, TWO fictional tenants, synthetic PDPA-safe data); `TenantIsolationIT` implementing **I-1…I-9 verbatim** from `06_TESTING_STRATEGY.md §5` (listing view scoped; foreign UUID ⇒ 404; foreign-FK write rejected; claim/slug mismatch ⇒ 401; no-context raw read ⇒ 0 rows; no-context raw insert rejected; app-role/BYPASSRLS canary; async listener tenant propagation; onboarding rollback).
**Acceptance/DoD**: all five slices green in one `./mvnw verify`; G-RLS evidence filed to `docs/ai-context/06_TESTING_EVIDENCE/` + `docs/security/RLS_TEST_STRATEGY.md` written; curl transcript of login→me→refresh→logout on dev profile; commits per slice. **G-RLS: no packet beyond P05 may start without this evidence.**

**P06 — CI/CD foundation** *(M; deps P05; remote steps gated by G-PUSH)* — `ci-backend.yml` (JDK17+m2 cache → `./mvnw verify` with Testcontainers → JaCoCo 70 % changed-lines → OWASP check fail-on-high → OpenAPI artifact upload), `ci-frontend.yml` (authored now, guarded until frontend exists: ci → lint → typecheck → test → build → audit), gitleaks step in both, Dependabot weekly (maven/npm/actions), `docs/deployment/BRANCH_PROTECTION.md` (main+dev: PR-only, 1 approval, required checks, no force-push) and `CD_PLAN.md` stub (dev auto → staging/prod manual per `03_ARCHITECTURE.md §12`). **Acceptance/DoD**: YAML lint-clean and committed; the day the remote exists (explicit user instruction required): test PR green with checks required, protection applied, evidence filed.

**P07 — Frontend shell** *(L; deps P05; gate G-ANEP on entry — CLEARED 2026-07-08, see `meeting/05_OUTCOME.md`)* — Vite React-TS scaffold; Tailwind, ESLint(+`no-literal-strings` on JSX)+Prettier, Vitest+RTL+MSW, react-i18next (en bundle, zero hard-coded copy); axios client (`withCredentials`, dev `X-Tenant-Slug` header) with request interceptor (memory access token) and response interceptor (single-flight silent refresh on 401 → retry once → logout); **auth store NOT persisted** (memory-only access token; refresh lives in the httpOnly cookie); router + `ProtectedRoute` + `<Can permission>` helper; `AppShell` layout with permission-filtered nav; Login, Dashboard placeholder, ForcedPasswordChange (must_change_password routing); `generate:api` script (openapi-typescript → `types.gen.ts`). **Acceptance/DoD**: lint/typecheck/test/build green; browser evidence of the six Phase-5 flows in `docs/ai-context/06_TESTING_EVIDENCE/browser_verification.md` (login happy/wrong-password/wrong-tenant, unauthenticated redirect, expired-token refresh, cross-tenant 404); types generated not hand-written.

**P08 — Property module** *(M; deps P05 backend / P07 frontend)* — Blocks CRUD, units CRUD (unique per tenant+block, status OCCUPIED/VACANT), delete-restriction (409 when block has units / unit has residents), Guard read = slim projection (unit numbers only); FE pages per journey J1 steps 2–3. **Acceptance/DoD**: pattern DoD + cross-tenant probe for the new resources + matrix rows (BLOCK_MANAGE, UNIT_MANAGE, UNIT_READ_ALL) + J1 property-portion browser evidence.

**P09 — People module** *(L; deps P08)* — User admin (create with one-time temp password + forced change, deactivate ⇒ refresh-family revoked, assign role), tenant role editing (`is_system` roles undeletable ⇒ 409), residents CRUD (optional user link; one-active-residency partial unique ⇒ 409), move-out (status change retains history, portal access revoked, occupancy recomputed), family members under own-scope (FAMILY_MANAGE_OWN), automatic unit occupancy hook both directions. **Acceptance/DoD**: pattern DoD + temp-password flow + deactivation token proof + occupancy transitions + same-tenant own-scope proof + full journey J1 in browser (admin builds property+staff+resident; resident logs in and sees their unit).

**P10 — Finance module** *(L; deps P09)* — Payment methods CRUD; bills issue/cancel/list (unique tenant+unit+month+type ⇒ 409 duplicate; cancel needs reason; paid uncancellable); payments record (**`SELECT … FOR UPDATE` on the bill; validate payable + amount ≤ outstanding; insert; recompute status — one transaction**); payments append-only (no update/delete endpoints; DB grants already revoked); `StorageService` + local-FS impl (first consumer): multipart ≤ 5 MB, jpg/png/pdf by magic bytes, generated keys, attachment-disposition downloads, own-scope on proofs; BigDecimal everywhere, JSON amounts as strings; events → notifications (new bill → unit residents; payment → payer) + audit; resident MyBills view. **Acceptance/DoD**: pattern DoD + `PaymentConcurrencyIT` (two parallel payments on a nearly-paid bill: exactly one succeeds, never negative balance) + partial→exact→PAID progression + upload rejection matrix + journey J2 in browser.

**P11 — Complaints module** *(L; deps P09)* — Categories CRUD; submission (resident auto-derived unit/residency; staff on-behalf); state machine OPEN→IN_PROGRESS→RESOLVED→CLOSED (+OPEN→REJECTED, resident reopen RESOLVED→IN_PROGRESS, resident close of own RESOLVED) implemented as a pure guard function `(current, target, actorRole, isOwner)` unit-tested over the whole matrix; every transition writes a `complaint_updates` row in-transaction (message mandatory on REJECTED/RESOLVED); notifications (OPEN → Managers; other transitions → the resident) via the P05 event pattern (tenant travels in the event payload); staff queue with filters, resident own-list, detail with full trail. **Acceptance/DoD**: pattern DoD + exhaustive transition-matrix tests + trail completeness (N transitions ⇒ N rows) + fan-out counts + own-scope 404 + journey J3 in browser. This module's guard pattern is the template for P12/P14.

**P12 — Maintenance module** *(M; deps P09; copy P11's pattern)* — Requests (resident own unit or staff on-behalf), priority LOW/MEDIUM/HIGH/URGENT, states PENDING→APPROVED→IN_PROGRESS→COMPLETED (+PENDING→REJECTED, message mandatory); queue filter status/priority/block, sort priority desc then age; notifications to requester on transitions, Managers on creation. **Acceptance/DoD**: pattern DoD + transition matrix + queue filter/sort correctness + own-scope + browser round-trip evidence.

**P13 — Dashboards & observability** *(M; deps P10, P11, P12)* — One `GET /api/dashboard`, role-shaped payload (Admin/Manager: open complaints, pending maintenance, unpaid/overdue bills, occupancy %; Resident: own balance + open items; Guard: placeholder until P16); custom metrics `baiti_login_failures_total`, `baiti_listener_failures_total`, `baiti_rls_missing_context_total`; `X-Request-Id` end-to-end; k6 scripts (load 50 VU/10 min: p95 <200 ms reads <400 ms writes, 0 errors; stress ramp 200 VU: find the knee; soak 20 VU/2 h: no pool exhaustion or memory creep; N+1 audit via Hibernate statistics) with baseline committed to `docs/testing/PERF_BASELINE.md`; trace drill (complaint → requestId → full trace ≤ 2 min) documented in `docs/deployment/OBSERVABILITY_RUNBOOK.md`. **Acceptance/DoD**: role payloads match fixtures; matrix rows; k6 targets met; drill timed and evidenced.

**P18 — Deployment & go-live** *(L; deps P13 + M5 exit + D3 decided ✅ Fly.io SG + G-PUSH long resolved)* — `Dockerfile.backend` (multi-stage, JRE-17 slim, non-root), `Dockerfile.frontend` (nginx static: SPA fallback, gzip, security headers), CD activation (dev auto → staging/prod manual approval), prod DB bootstrap mirrors the two-role model (**verify the I-7 canary on the production database — managed-Postgres role semantics are where this breaks silently**), platform secret store + scripted `.env.example` parity check, prod profile flips to `SubdomainTenantResolver` (wildcard DNS+TLS), nightly `pg_dump` to off-site storage 30-day retention, `restore_drill.sh`, runbook (start/stop/scale, logs, trace drill, JWT rotation = global logout, restore, tenant-onboarding SOP, incident ladder), go-live checklist. **Acceptance/DoD**: staging smoke = scripted J1–J3 with two fresh tenants; restore rehearsal timed (RTO ≤ 4 h proof); rollback rehearsal (deploy N-1, verify, redeploy N); prod seed-free; first real tenant onboarded via runbook; backups observed two consecutive nights; checklist signed line-by-line.

**P14 — Facilities module** *(L; E7, after P18; technical dep P09)* — Facilities CRUD (status AVAILABLE/MAINTENANCE/CLOSED blocks new bookings); bookings PENDING→APPROVED/REJECTED (+CANCELLED by booker before slot start), approver recorded; **double-booking settled by the partial unique index** (tenant, facility, date, slot) WHERE status IN (PENDING, APPROVED) — service catches the violation ⇒ 409 SLOT_TAKEN, never pre-check-only; decision notifications to booker. **Acceptance/DoD**: pattern DoD + `BookingRaceIT` (parallel same-slot ⇒ exactly one) + state matrix + own-scope + status blocking + browser round-trip.

**P15 — Community module** *(M; E7; technical deps P09 + P10's StorageService)* — Announcements (title/content/attachment/audience ALL|OWNERS|TENANTS, **audience enforced as a server-side query predicate from the caller's residency type**, staff see all); events CRUD (end ≥ start, DB CHECK + validation); resident feed (posted_at desc) + events past/upcoming split. **Acceptance/DoD**: pattern DoD + targeting proofs both directions + attachment rules + browser evidence of audience-correct visibility.

**P16 — Visitors module** *(L; E7; technical dep P09)* — Pre-registrations with server-generated 8-char unambiguous-alphabet approval codes (unique per tenant, shown once); gate verification: code valid ∧ PENDING ∧ visit_date = today ⇒ create visitor (entry_time now, linked `pre_registration_id`) + mark USED, one transaction; wrong/used/expired ⇒ generic 404-shaped failure (no code oracle); walk-ins direct against a host resident; check-out stamps exit_time; on-site list = entered-not-exited (partial index); nightly expiry PENDING→EXPIRED; Guard payloads are slim projections (name + unit — no phone/email: decision baseline, SRS §8.5). **Acceptance/DoD**: pattern DoD + single-use + date-window + on-site correctness + Guard field-level projection assertion + non-enumerability + journey J4 from both sides.

**P17 — Notification centre & scheduled jobs** *(M; E7; technical dep P10)* — Notification list / mark-read / read-all / unread-count (badge polls via TanStack Query); own-scope on reads/marks; `NotificationChannel` SPI with `LogOnlyEmailChannel` stub (proves the seam; delivers nothing); scheduled-job infrastructure (also hosts P16's expiry): **jobs iterate tenants explicitly, setting/clearing tenant context per tenant** — ThreadLocals do not exist in schedulers; `OverdueBillsJob` nightly (UNPAID past due ⇒ OVERDUE + notify unit residents, idempotent on re-run); arrears view `GET /api/bills/arrears`; shedlock noted only if replicas ever exist. **Acceptance/DoD**: pattern DoD + job idempotency + per-tenant job scoping proof + own-scope marks + badge correctness + clock-forced overdue transition in browser.

## 8. Module Dependencies (machine-checkable)

```
P00 ◀ P01 ◀ P02 ◀ P03 ◀ P04 ◀ P05
P05 ◀ P06        (CI without the isolation suite certifies nothing)
P05 ◀ P07        (FE auth needs real endpoints)   [+ gate G-ANEP: CLEARED]
P05 ◀ P08 ◀ P09 ◀ P10       (people screens need property; finance needs both)
P09 ◀ P11        P09 ◀ P12
P10,P11,P12 ◀ P13 ◀ P18 ◀ {P14, P15, P16, P17}
```
Inside every feature module: backend slice (tests green, contract frozen) before frontend slice. Two-person parallelism from M3: one runs packet-N backend while the other runs packet-(N-1) frontend.

## 9. Recommended Execution Order (from the current position)

0. Close P01 residue: start Docker Desktop (if hung: `wsl --status`; update 4.78→4.81; reboot) → `docker ps` → `docker compose -f infra/docker-compose.yml up -d` → role checks (`current_user`=baiti_app; `rolbypassrls`=f) → append evidence to `docs/audits/PHASE0_VERIFICATION.md`. Run the folder rename when no session locks it: `Rename-Item "C:\Users\asbou\Desktop\Work\Development\Anep's Project (Baiti)" -NewName "baiti"`.
1. P02 → 2. P03 → 3. P04 → 4. P05 (slices a–e strictly in order) → **G-RLS checkpoint** → 5. P06 → 6. P07 → 7. P08 → 8. P09 → 9. P10 → 10. P11 → 11. P12 → 12. P13 → **M5 hardening activities** (k6, security pass, UAT per §12–13) → 13. P18 → **v1.0 live** → 14. P14 → 15. P15 → 16. P16 → 17. P17. One packet per agent-session; never two in flight.

## 10–11. Acceptance Criteria & Validation Requirements (programme-wide)

Module-level acceptance criteria are in §7 per module (and in full detail in each packet file). Programme-wide validation rules, binding on every module:
- Every FR implemented gets Given/When/Then criteria written **before** code; the failing test is named after the criterion.
- **Evidence before claims** (verification-before-completion): no done-claim without captured command output/screenshot in the PR/session notes; milestone evidence additionally filed to `docs/ai-context/06_TESTING_EVIDENCE/`.
- Every new endpoint gets: authz-matrix rows for all four roles, an own-scope proof if `*_OWN`, and a cross-tenant 404 probe.
- Bug fixes reproduce with a failing test first; the test remains as regression armour.
- Flaky tests: quarantine ≤ 7 days with an issue; a quarantined isolation/authz test blocks release regardless.

## 12. Testing Strategy (full text: `docs/blueprint/06_TESTING_STRATEGY.md`)

Pyramid: backbone = **backend integration slice tests on real PostgreSQL via Testcontainers** (RLS/constraints/transactions do not exist on mocks or H2; singleton container pattern; whole `./mvnw verify` ≤ 5 min or it is a defect) · backend unit (state-machine guards, BigDecimal maths, validators) · frontend Vitest+RTL with MSW fixtures generated from OpenAPI examples (no snapshot-only tests) · ArchUnit (architecture as tests) · contract (OpenAPI breaking-change diff per PR) · migration test (schema+RLS+grants assertions) · Playwright E2E post-MVP, thin, critical journeys only. Special suites: **isolation I-1…I-9** (defined verbatim in 06 §5 — permanent release gate), **authz matrix** (parametrised over the full grid with endpoint-enumeration guard), **auth abuse set** (JWT tampering, rotation replay). System testing per milestone = journey scripts J1–J5 against a deployed build, evidence in the existing templates. UAT pre-v1.0: per role × module CRUD, every state machine to every terminal state, billing lifecycle with partials, cross-tenant spot checks; entry = I-suite green + current system-test evidence; exit = zero SEV-1/2 open, sign-off filed. Performance (M5): k6 load/stress/soak per P13 with recorded baseline; >20 % regression fails release. Security: OWASP dep-check + npm audit (high/critical fail CI), gitleaks every commit, manual OWASP Top-10 pass per milestone release, JWT abuse cases kept forever.

## 13. Quality Gates

**Decision gates**: G-NAME ✅ (rename approved; physical execution pending) · G-ANEP ✅ (`meeting/05_OUTCOME.md`) · G-PUSH — repo target decided (`FaisalHanafi98/baiti`, private) but **any repo creation/push requires a fresh explicit user instruction** · **G-RLS** — open; closes only with I-1…I-9 green + filed evidence; blocks P06+.
**CI gates (every PR, once CI exists)**: all tests green, zero unexplained skips · JaCoCo ≥ 70 % / Vitest ≥ 60 % changed lines · migration + isolation + authz suites · ArchUnit · OpenAPI diff · Spotless/ESLint · gitleaks · OWASP/npm audit high-fail.
**Release gates**: k6 baseline regression < 20 % · restore rehearsal (go-live) · go-live checklist signed.
**Process gates**: commit protocol (draft → Faisal approves → commit; message per CREAMS SOP — `.gitmessage`, `[Assisted by AI, reviewed manually by Faisal]` footer, no AI Co-Authored-By trailers); session-end ai-context updates.

## 14. Engineering Decisions (decision · rationale · alternatives · constraints/assumptions · impact)

| # | Decision | Rationale | Alternatives rejected | Impact / reversal path |
|---|---|---|---|---|
| ED-1 | Stack: Spring Boot 3/Java 17+/PostgreSQL 16/React 18+TS+Vite | type safety over 26 relations; **RLS is the Postgres reason**; React Native path; hireability (full argument: `meeting/02_STACK_RATIONALE.md`) | Laravel (portfolio repetition), Node/Express (weaker relational ORM), Django (weaker multi-tenant story), Vue/Angular/Next (mobility, weight, no SSR need), MySQL (no RLS), MongoDB (relational data) | locked (CON-1); reversal = new programme |
| ED-2 | Modular monolith, monorepo (ADR-0002) | 2 part-time devs; transactional simplicity; contract lockstep | microservices, k8s, split repos | extraction seams kept via ArchUnit + interfaces |
| ED-3 | Shared-schema tenancy + 3-layer isolation (JWT → `@TenantId` → RLS) | cheapest model with DB-enforced guarantees | schema-per-tenant (migration fan-out, pool fragmentation at 50 tenants), app-layer-only filtering (one bug from a leak) | whale-tenant extraction path at the datasource seam |
| ED-4 | **Dual DB roles + FORCE RLS** | PostgreSQL RLS does not bind table owners — single-role designs silently void isolation (defect found in legacy plan) | single role (broken), BYPASSRLS app role (nonsense) | roles from infra bootstrap; I-7 canary forever; prod DB must be re-verified (P18) |
| ED-5 | Tenant GUC via datasource wrapper set/reset on borrow/return | `SET LOCAL` no-ops outside transactions (legacy design was a no-op); pool-safe; testable | SET LOCAL interceptor (broken), Hibernate multi-tenancy SPI connection provider (heavier, same effect) | connection-bleed test guards it |
| ED-6 | `current_setting(..., true)` + missing-context ⇒ NULL ⇒ fail closed | absence of context must never mean "see everything" | erroring policies (breaks global paths noisily) | `baiti_rls_missing_context_total` metric watches for silent bugs |
| ED-7 | JWT 15-min access + rotating opaque refresh, hashed, **family revocation on reuse** | caps stolen-token utility; replay detection | long-lived JWTs, non-rotating refresh, server sessions (kills statelessness/mobile) | env flag can disable rotation as break-glass (risk RR-8) |
| ED-8 | Browser tokens: access in memory only; refresh in httpOnly SameSite=Strict cookie; body fallback for mobile | localStorage token = one XSS from takeover; caps exposure at 15 min | zustand-persisted localStorage (simpler, rejected) | CORS with credentials required; documented in P07 |
| ED-9 | Separate `super_admins` table + separate filter chain | platform operators inside tenant `users` would need NULL-tenant exceptions in every policy — standing foot-gun | role-flag on users, shared table with nullable tenant | tenant compromise cannot reach cross-tenant surface |
| ED-10 | Role/permission **templates live in code** (`PermissionCatalog` enum), copied per tenant at onboarding, startup sync per release | catalog must match `@PreAuthorize` constants — in code, drift is a compile error; kills 3 global tables + their RLS exceptions | template tables (drift risk, RLS exceptions), global-only permissions (tenants couldn't customise) | releases adding permissions must ship the sync (P05c) |
| ED-11 | Enums = TEXT + CHECK, not native PG enums | value evolution = constraint swap in a new migration, not `ALTER TYPE`; trivial JPA mapping | native enums | none |
| ED-12 | Audit columns (`created_by`/`updated_by`) are plain UUIDs, NOT FKs; domain user references ARE FKs | audit stamps ≠ relational integrity; dissolves migration-order coupling; system rows have no user | FKs everywhere (ordering knots, delete coupling) | joins to users are best-effort for audit display |
| ED-13 | Money: `NUMERIC(12,2)` + `BigDecimal` + JSON strings; MYR-only with `currency CHAR(3) DEFAULT 'MYR'` | float money is a defect class; currency column makes multi-currency a data change | float/double (never), integer cents (workable, less readable in SQL) | D4 reversible without migration |
| ED-14 | EN-only with react-i18next scaffold from first screen; backend returns error codes, frontend owns wording | BM (D5) becomes a translation file, not a refactor | hard-coded strings (rework), full bilingual now (scope) | ESLint no-literal-strings enforces |
| ED-15 | No caches, no message broker at v1.0; in-process Spring events + @Async; **outbox-first rule** if delivery guarantees ever needed | correctness first; RLS makes cross-tenant cache keys dangerous; scale doesn't warrant a broker | Redis cache, RabbitMQ/Kafka | any addition requires an ADR + profiler trace |
| ED-16 | Dev seeding via `DevDataInitializer` calling the real onboarding service; **no SQL seed migrations** | a SQL twin of onboarding logic would drift; seeding exercises production code | V-numbered seed migrations (legacy plan) | two synthetic tenants always (isolation tests need ≥ 2) |
| ED-17 | Cross-tenant = 404, never 403 | 403 confirms existence — an oracle | 403 (leaks) | test suites assert 404 shape |
| ED-18 | Payments and activity_logs append-only, enforced at endpoints AND DB grants | financial/audit integrity; corrections = compensating entries | soft-editable rows, DB triggers for balance (logic split, untestable) | balance invariant via `FOR UPDATE` in service tx |
| ED-19 | Booking/residency races settled by partial unique indexes, not app checks | the DB is the only serialisation point that can't be raced | check-then-insert (racy) | service translates violations to 409 |
| ED-20 | Deployment: Fly.io Singapore (D3, user-approved 2026-07-08) | SG region = MY latency; Docker-native; ~US$5–10/mo | Railway (no SG), Render (no SG), VPS (own the ops burden) | executed at P18; matrix retained in 03 §12 |
| ED-21 | Commit messages per CREAMS SOP (user directive 2026-07-08) | one commit voice across Faisal's projects | plain conventional commits + AI Co-Authored-By trailer (explicitly rejected) | `.gitmessage` template; CLAUDE.md §commit |
| ED-22 | Naming BAiTI everywhere new (ADR-0001); legacy files keep names for traceability | rename landed before line 1 of code — zero rename debt | retrofit later (nightmare) | new MyComunity-named artifacts are review-rejections |

Product-decision baselines (decided-but-reversible, recorded in `docs/blueprint/00_REPOSITORY_INVENTORY.md §5` + `01_SRS.md §8`): single bill type + reserved `bill_type` column · Guards see name+unit only · uploads 5 MB jpg/png/pdf · `family_members.age` kept (DOB later) · single role per user (pivot escape path documented).

## 15. Rollback Strategy

| Stage | Rollback |
|---|---|
| Now → first push (local-only repo) | `git revert`/`git commit --amend`; nothing external exists — cheapest change window (why the security core sits here) |
| Schema, always | **forward-fix only** — new `Vn+1` migration, never edit applied ones; pre-deploy backup mandatory; expand-migrate-contract for live column changes; deploy checklist asks "does this migration break the previous app version?" |
| App releases (M6+) | previous image tag retained; rollback = redeploy previous tag; rehearsed before go-live |
| Feature misfire in prod | no flag framework at v1.0 (deliberate); rollback = previous tag; single env flag exception: refresh-rotation disable |
| Failed go-live | restore + rollback drills happen BEFORE DNS cutover, on staging and prod infra |

## 16. Deferred Work (with reasons, priority, dependencies, impact)

| # | Item | Reason deferred | Pri | Deps | Impact if never done |
|---|---|---|---|---|---|
| 0 | **Docker daemon verification + compose role checks** (P01 residue) | Docker Desktop 4.78 daemon unresponsive; user paused it 2026-07-08 | **Blocking for P02** | user machine | programme cannot proceed |
| 0b | **Folder rename to `baiti`** | live session held a file lock | High, cosmetic | no session holding the dir | path/name inconsistency in docs |
| 1 | Docker Desktop update 4.78→4.81 | may fix item 0 | High | — | — |
| 2 | `git config commit.template .gitmessage` | local config, one-liner | Low | — | template unused |
| 3 | Password reset by email (FR-AUTH-07) | needs email channel | Med | P17 SPI | admins reset manually |
| 4 | Per-role session policy (shared Guard gate devices) | global TTL acceptable v1.0 | Low | P16 usage | slightly weaker gate-device hygiene |
| 5 | `family_members.age` → `date_of_birth` | PDF fidelity kept v1.0 | Low | v1.x migration | ages go stale |
| 6 | Facility slot table (vs labels) | labels sufficient v1.0 | Low | P14 data | limited slot tooling |
| 7 | Complaint photo attachments | StorageService lands P10 | Med | P10, P11 | text-only complaints |
| 8 | Blue/green deploys | single-node stop-start accepted (99.5 % SLO) | Low | post-v1.0 | brief deploy downtime |
| 9 | activity_logs partitioning + retention job | trigger ≈ 5 M rows | Low | volume | table growth |
| 10 | Payment gateway (FPX/Billplz/toyyibPay) | manual recording is v1.0 scope | Med | post-v1.0 | staff key in payments |
| 11 | WAL/PITR backups | nightly dumps meet RPO 24 h | Med | post-v1.0 | RPO stays 24 h |

## 17. Future Roadmap (post-M7, direction only — not commitments)

React Native mobile app (auth/api/hooks/types port as-is; refresh-in-body path already built) · payment gateway epic · email/SMS channels via the P17 SPI + outbox · Bahasa Malaysia bundle · tenant plan tiers (`tenants.plan` column + per-tenant permission templates already support it) · reporting/exports · accessibility audit automation (axe in Playwright) · read replicas → partitioning → whale-tenant DB extraction (in that order, each with an ADR).

## 18. Known Risks (consolidated register — supersedes all earlier lists)

| ID | Risk | L×I | Mitigation |
|---|---|---|---|
| RR-1 | Cross-tenant data leak | L×CRIT | 3 layers; I-suite as permanent gate; 404 rule |
| RR-2 | RLS silently not binding (owner/BYPASSRLS misconfig) | M×CRIT | dual roles + FORCE + I-7 canary every build **and on prod DB at P18** |
| RR-3 | Tenant context bleed across pooled connections | M×CRIT | reset-on-return; bleed test; fail-closed proofs |
| RR-4 | Tenant lost in async/scheduled work | M×H | tenantId in event payloads; per-tenant job loops; I-8 |
| RR-5 | Anep expectations diverge later | M→L×H | outcome recorded (05_OUTCOME); reopen protocol noted there |
| RR-6 | Part-time capacity / scope creep | H×M | MoSCoW priorities; packet sizing; v1.x gated behind go-live |
| RR-7 | Slow Testcontainers on Windows kills TDD | M×M | singleton container; 5-min suite budget is a defect threshold |
| RR-8 | Refresh-rotation races lock users out | L×H | family model; parallel-tab tests; break-glass env flag |
| RR-9 | Legacy PDF/plan defects re-enter via careless generation | M×M | deviation register (04 §9) normative; grep checks in P03 DoD |
| RR-10 | Editing applied migrations | M×M | immutability rule; Flyway checksum fails CI |
| RR-11 | PDPA: real personal data committed | L×H | synthetic-only; gitleaks; log-redaction reviews |
| RR-12 | Docker/toolchain instability on the dev machine | M(observed)×M | evidence-first P00 protocol; §16 item 0 |
| RR-13 | Two-person bus factor / session amnesia | M×M | blueprint + ai-context protocol + this plan + CURRENT_MODULE_STATE.md |
| RR-14 | JWT secret leak | L×CRIT | env-only; scanner; rotation runbook (cost: global logout) |

## 19. Success Criteria

**v1.0 is successful when**: all M0–M6 exit criteria evidenced; journeys J1–J3 pass in production with a real tenant; the isolation suite is green on the production database; NFR budgets hold under the k6 baseline; zero SEV-1/2 open; backups verified restorable; both collaborators can operate the runbook. **The programme is successful when** additionally M7 lands (J4 passes) and the codebase remains at its quality bars (coverage, ArchUnit, contract diff) without exception creep.

## 20. Definition of Done — per phase

- **A packet/module is done** when: its DoD checklist (§7 / packet file) is fully evidenced · all programme-wide validation rules (§10–11) met · isolation suite still green · docs updated (`docs/ai-context/01_CURRENT_STATUS.md`, session history, `CURRENT_MODULE_STATE.md`) · commits user-approved in SOP format · nothing outside the packet's scope was touched.
- **A milestone is done** when: every contained packet is done · the milestone exit criteria (§5) are evidenced in `docs/ai-context/06_TESTING_EVIDENCE/` · its integration checkpoint demonstrated · gates crossed with evidence filed.
- **A release (M6+) is done** when: CI gates + release gates (§13) pass · rollback path rehearsed for that release · runbook updated · go-live/release checklist signed line-by-line with evidence links.

---

## Self-verification (final checklist)

✓ Every epic documented (E1–E7, §6) · ✓ every module documented (all 19 packets, §7 — none merged or omitted) · ✓ every dependency (§8) · ✓ execution order incl. current-position step 0 (§9) · ✓ acceptance criteria per module (§7) + programme-wide validation (§10–11) · ✓ integration checkpoints (§5) · ✓ testing strategy (§12) · ✓ quality gates (§13) · ✓ engineering decisions with rationale/alternatives/impact (ED-1…ED-22 + product baselines, §14) · ✓ rollback (§15) · ✓ deferred work (§16) · ✓ future roadmap (§17) · ✓ risks (§18) · ✓ success criteria (§19) · ✓ DoD per phase (§20). Conversation-only knowledge check: environment specifics, Docker deferral, rename lock, commit-SOP directive, decision approvals, and all discovered legacy-plan defects are externalised here and in the committed blueprint/handover files. Nothing required to continue exists outside the repository.
