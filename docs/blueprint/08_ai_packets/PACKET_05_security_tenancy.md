# P05 — Security & Tenancy Core (XL — execute as five sequential slices)

## Slice 5a execution checkpoint — 29 October 2026

Given an owned native PG16 database with named synthetic A/B communities, setting
TenantContext must scope raw JDBC and Hibernate to that tenant. Returning the same
physical connection must clear its setting, roll back unfinished work and recover
an aborted transaction. Missing context must return zero rows/reject insertion;
binding/reset/rollback failures must evict the connection. A request must use its
resolved tenant only, reject invalid/unknown/suspended resolution generically and
clear context after success or exceptions. Production resolution uses the explicit
APP_BASE_DOMAIN and ignores forwarded headers. Global lookup must use the marked
owner pool while the primary app path remains baiti_app.

These cases pass in 208 local tests; see
[slice evidence](../../ai-context/06_TESTING_EVIDENCE/2026-10-29_tenant_context.md).
No DDL or role changes. The pre-authentication resolver order follows 02 §4.2's
login prerequisite; JWT matching follows in 5b. The 5a owner lookup seam does not
implement 5d's caller boundary or onboarding transaction. Docker/compose execution
is replaced by the accepted native plan; hosted parity remains deferred. The
owner's continuing local-commit instruction supersedes a separate message-approval
pause. P05 as a whole and G-RLS remain unfinished.

**Objective**: The load-bearing packet. Tenant context propagation to PostgreSQL, JWT auth with rotating refresh, RBAC enforcement, super-admin plane, atomic tenant onboarding, dev seeding — finished by the full isolation suite. **Exit = gate G-RLS**: nothing after this packet may start until I-1…I-9 are green in CI-equivalent runs.

**Dependencies**: P04. **References**: 03_ARCHITECTURE §6–§7 (normative), 02_SYSTEM_ANALYSIS §4.1–4.2 & §8, 06_TESTING §3.4, §5.

---

## Slice 5a — Tenant context & fail-closed datasource
Files: `tenancy/{TenantContext,TenantResolver,HeaderTenantResolver,SubdomainTenantResolver,TenantContextFilter}.java`, `config/TenantAwareDataSource.java`, Hibernate `CurrentTenantIdentifierResolver` impl.
- `TenantContext`: ThreadLocal holder; `clear()` in filter `finally`. Resolver chosen by profile (03 §6.4).
- `TenantAwareDataSource` (03 §6.3): wraps Hikari; on borrow `set_config('app.current_tenant', ctx, false)` or `RESET`; proxied `close()` always `RESET`s before pool return.
- `@TenantId` resolver reads `TenantContext` (activates the layer-2 filter declared in P04).
Tests: I-5 and I-6 (raw JdbcTemplate, no context ⇒ 0 rows / rejected insert); context-bleed test (borrow-set-return-borrow on 1-connection pool ⇒ second borrow sees no context); I-7 canary (`current_user='baiti_app'`, `rolbypassrls=false`, cross-tenant select ⇒ 0).

## Slice 5b — JWT authentication
Files: `security/{JwtService,JwtAuthenticationFilter,SecurityConfig,AuthController,AuthService,RefreshTokenService,LoginRateLimiter}.java` + DTOs.
- Endpoints `POST /api/auth/login|refresh|logout`, `GET /api/auth/me` (FR-AUTH-01…06). SecurityConfig per refresher §6 idiom: stateless, CSRF off, `/api/auth/**` permitAll, health permitAll, all else authenticated.
- Access token: 15 min HS256, claims `userId,tenantId,roles,permissions,jti`; secret from env only.
- Refresh: opaque 256-bit random → SHA-256 into `refresh_tokens`; rotation + family revocation on reuse (03 §7.2); web = httpOnly Secure SameSite=Strict cookie on `/api/auth/refresh`, body fallback accepted (03 §7.4). CORS allowlist with credentials.
- Rate limiting: bucket4j per-IP and per-(tenant,email) on login (429).
Tests: full 06 §3.4 suite + JWT abuse set (expired/bad-signature/`alg:none`/tampered tenant claim) + generic-401 assertions (same body for wrong-password / no-user / suspended tenant / unknown slug).

## Slice 5c — RBAC enforcement
Files: `people/PermissionCatalog.java` (enum = the machine copy of 02 §8), `people/DefaultRoleGrants.java`, `security/PermissionEvaluator` wiring, `@EnableMethodSecurity`.
- Login resolves role→permissions into claims; `hasAuthority('<CODE>')` guards; own-scope helper (`ResidentScopeGuard`) for `*_OWN` service predicates.
- Startup `PermissionSyncService`: idempotently creates missing catalogue rows and default roles per tenant (04 §2 rationale). Existing system roles receive default grants only for newly created catalogue codes; preserve Admin-revoked grants for existing codes and all custom-role assignments (FR-RBAC-02, clarified 4 November). Serialise each tenant's transaction; initial defaults for a newly created role are complete.
Tests: authz-matrix harness (06 §5.3) with fixture table seeded from 02 §8; endpoint-enumeration assertion armed (any future unclassified endpoint fails).

Execution checkpoint, 4 November: 42 catalogue codes, exact default-role fixture,
scoped/idempotent/concurrent sync, named method authorities and persisted tenant
actor stamps pass 302 native tests. Sync follows the clarified FR-RBAC-02 policy;
the planning reconciliation below is now resolved for this implementation.
The matrix's 168 permission decisions use a labelled test-only probe, while
handler/OpenAPI enumeration covers actual application endpoints. Own-scope and
future business endpoint permissions remain required; this is not full 5c/G-RLS.
See [evidence](../../ai-context/06_TESTING_EVIDENCE/2026-11-04_permissions.md).

### Planning baseline and acceptance map — 4 November 2026

Status: planning only; no application implementation or test run in this checkpoint.
Checked `igumi` at `4ce314b` in `backend/.local/github-publication`. The worktree
was clean at intake; local main/igumi/anep refs matched. Live GitHub refs were not
queried. The original detached checkout retains 84 local changes; do not copy its
research and file moves into this checkout as a bulk change.

The supplied `workspace/reels-improvement-2026-10-19/implementation/BAiTI/00-PROJECT-WORKFLOW.md`
is a planning handoff. BT03 alignment and BT01's remaining security are the
immediate prerequisites. BT-M01, BT-A1 and BT-A2 are proposed, dependency-gated
extensions; no product module is selected or implemented by this checkpoint.
The landed-RA pilot and later commerce direction stand. New procurement or
financial rules must enter the owning blueprint before changing domain behavior.

| Requirement | Inspected source / evidence | Classification and acceptance still needed |
|---|---|---|
| Tenant resolution, JDBC/Hibernate binding and cleanup | `tenancy/`, `config/TenantAwareDataSource.java`; `TenantConnectionIT`, `HibernateTenantIT`, filter/resolver tests | Source exists; dated native verification. Preserve A/B positive controls, missing-context denial, physical reuse and unfinished/aborted transaction cleanup. Not rerun today. |
| Login, JWT tenant matching, refresh/logout/me | `security/`; `people/UserAuthenticationService.java`; `AuthenticationFlowIT`, `JwtAuthenticationIT` | Source exists; dated native verification. Role permission codes already enter identities/JWTs. Preserve these flows rather than replace authentication. |
| Catalogue and default-role definitions | Existing `people/Permission`, `Role`, `RolePermission` and repositories; no `PermissionCatalog` or `DefaultRoleGrants` found in main/test source | Missing application catalogue. Test exact code/default-grant parity with 02 §8; expand grouped/shorthand matrix entries explicitly. Admin is not an implicit wildcard; Guard permissions do not remove field-projection restrictions. |
| Catalogue/default synchronisation | No `PermissionSyncService` found | Missing. Native tests must prove scoped, idempotent creation, correct new-tenant defaults, rollback, concurrent/retry behavior and preservation of Admin-edited assignments. Resolve the reconciliation below before implementing startup writes. |
| Method permissions and endpoint classification | `config/SecurityConfig.java` authenticates tenant routes and denies `/api/admin/**`; no method-security enablement or `@PreAuthorize` found | Partial. Add allowed/denied authority cases and endpoint enumeration from actual generated OpenAPI. Explicitly classify auth/health/infrastructure routes; do not grant business permissions to those routes just to satisfy the matrix. Test-only probes are not completed product APIs. |
| Same-tenant own-record access | No `ResidentScopeGuard` found; tenant RLS and role grant validation already exist | Missing service predicate. Resident 1 must not read Resident 2's resource in the same community; own-resource and authorised staff positive controls must also pass. Future endpoints each need their own proof. |
| Actor fields | `config/JpaConfig.java` returns `Optional.empty()`; `common/TenantScopedBaseEntity.java` declares CreatedBy/LastModifiedBy | Explicit stub. Tests must trace a trusted matching TenantPrincipal to persisted actor UUIDs; anonymous, unrelated principal, foreign/missing tenant context and cleanup must not reuse a previous actor. Platform actor semantics belong to 5d. |
| Platform onboarding | Owner pool/slug lookup exists; no `TenantOnboardingService` or super-admin login/controller found | Partial infrastructure only. Separate platform identity, atomic onboarding, duplicate-slug behavior and forced failure rollback remain 5d. Existing platform SQL denial tests are not onboarding evidence. |
| Async audit, real seed and full isolation suite | No async audit listener, real onboarding seed or `TenantIsolationIT` found | Missing behavior. Complete I1–I9 using real onboarding and documented entry-point seams; do not relabel foundation fixtures as the full gate. |
| Finance / product extensions / frontend | Finance and maintenance entity/repository seams; no frontend directory | Contracts/scaffolding are not working finance/procurement/retrieval journeys. Full G-RLS and the relevant property/people/finance/maintenance and RA-domain decisions remain prerequisites. |

**Default-grant reconciliation:** FR-RBAC-02 seeds defaults at tenant creation
and lets Admins edit assignments. Restoring every absent default link on each
startup could silently regrant a permission that an Admin revoked. Define and
test how initial bootstrap, new catalogue entries and existing customised roles
are distinguished. Existing grants/revocations must not be overwritten merely
because `Role.isSystem` is true. Record the chosen reconciliation in this packet
and its owning SRS/architecture contract before enabling startup sync. This is a
design issue to resolve, not evidence that a currently implemented sync is broken.

**Next bounded implementation:** start with catalogue/default-role definitions
and independently authored matrix fixtures from 02 §8. Establish a meaningful
red test for complete permission/default parity, then implement the minimal
in-memory definitions. Include grouped code expansion, explicitly withheld
grants and separation of platform permissions. No DDL, startup writes, new
product endpoint, frontend or role-policy expansion is part of that first slice.
Pure catalogue green does not finish 5c or close G-RLS.

Then implement the resolved tenant-scoped sync, method/own-scope enforcement and
actor auditing in coherent tested steps before 5d/5e. Use actual app-role native
tests for persisted behavior. The existing seven-key runner accepts scoped
integration selection; the full relevant suite still precedes completion.

**Execution baseline:** Java 21.0.11 and Maven 3.9.16 are available; the POM
declares Boot 4.1.2, springdoc 3.2.0 and Java source 17. There are 31 source
migrations, ending at V31; none was changed or applied here. `pg_ctl status`
reported no server running. The existing private P04 settings path exists,
but its values were not read and no ownership/role preflight or DB connection
was made. Before a DB-dependent implementation run, recheck the exact owned
target and roles through the existing runner; never infer ownership from a
filename or database-name prefix. No restart, reset or provisioning occurred.
The 276-test result remains dated 3 November, not a new result for this review.

Author source review used the backend/security/verification guidance; no
independent reviewer or security clearance is claimed. Keep `main`, `igumi`,
`anep`; this handoff authorises no new push, PR, merge or branch. Docker,
hosting/VPS and deployment remain deferred. See the
[session record](../../ai-context/02_SESSION_HISTORY/2026-11-04_workflow_baseline.md).

## Slice 5d — Super-admin plane & tenant onboarding
Files: `tenancy/admin/{SuperAdminAuth…,TenantAdminController,TenantOnboardingService}.java`, privileged-datasource seam (`@UsesPrivilegedDataSource` annotation + second `JdbcTemplate`/`EntityManager` on the migrator datasource, 03 §6.5).
- `POST/GET /api/admin/tenants` on a separate filter chain requiring a SuperAdmin principal (separate login `POST /api/admin/auth/login`).
- Onboarding (02 §4.1): single transaction — tenant row, roles/permissions copies from catalog, first Admin user (`must_change_password=true`), TENANT_CREATED audit; idempotent by slug (409, zero writes).
Tests: I-9 (forced mid-failure ⇒ full rollback); duplicate slug; tenant-admin JWT cannot call `/api/admin/**` (403); ArchUnit: `tenancy.admin` imports no domain repositories.

## Slice 5e — Dev seed + the isolation suite
Files: `config/DevDataInitializer.java` (`@Profile("dev")`, idempotent, calls real onboarding for two fictional tenants with synthetic-only data per `docs/ai-context/04_DATABASE_STATE/seeders_status.md` PDPA rules), `test/…/TenantIsolationIT.java`.
- Implement I-1…I-9 (06 §5) verbatim as named test methods.
Exit evidence: full `./mvnw verify` output + the I-suite listing filed to `docs/ai-context/06_TESTING_EVIDENCE/` and `docs/security/RLS_TEST_STRATEGY.md` written (one page: what the suite proves, how to extend it per new table).

---

**Definition of Done (packet)**
- [ ] All five slices' tests green in one `./mvnw verify`; coverage gate holds.
- [ ] G-RLS evidence filed; `docs/ai-context/01_CURRENT_STATUS.md` flips the milestone.
- [ ] Login→me→refresh→logout demonstrated with curl transcript against dev profile + compose DB (two seeded tenants).
- [ ] No secret material in repo; JWT secret consumed from env; commits sliced 5a–5e, each user-approved.

### Slice 5b access-token checkpoint — 29 October 2026

Given a correctly signed tenant access token and matching resolved active slug,
a protected route succeeds without a session. Missing/foreign context and malformed,
expired, unsigned, altered or wrong-key tokens return 401 before the route; a
valid tenant token on the platform path returns 403. Required claims and exact
15-minute expiry are checked. No default generated user or basic/form fallback.
These cases pass in 238 native tests; see
[access evidence](../../ai-context/06_TESTING_EVIDENCE/2026-10-29_jwt_access.md).
This is part of 5b, not completion: password verification, opaque refresh rotation,
family-reuse revocation, logout/me, rate limits, cookie CSRF and CORS remain next.
CSRF is disabled only for the current bearer-only authenticated surface; any
cookie refresh endpoint must add its explicit origin/header contract in that change.

### Slice 5b workflow checkpoint — 30 October 2026

Given an active community/user and valid bcrypt password, login issues access and
an HttpOnly refresh cookie; only its SHA-256 hash is stored. Wrong password,
unknown user/slug, inactive user and suspended community return identical generic
401 bodies. Rotation preserves the family and locks the token row; presenting a
rotated token commits family revocation before 401. Concurrent refresh has one
winner and revokes that replacement when the losing replay is detected. Logout
revokes every user refresh family; me/logout require bearer authentication.

Cookie refresh requires allowed Origin plus X-BAiTI-Refresh: true. Body fallback
uses an explicit token without a cookie. CORS is explicit with credentials;
unsupported origins are rejected at the CORS protocol layer. Application auth
errors use RFC 7807. Login IP/account limits, a separate refresh IP budget, bounded
local cache, 8 KB auth-body cap and the bcrypt 72-byte limit are exercised.
Configurable whole-second lifetimes cannot exceed the published v1 limits of
15 minutes / 7 days; increasing those limits needs a documented contract change.
Only login/refresh are anonymous, refining the packet's broad /api/auth/**
shorthand to preserve FR-AUTH access requirements. Native execution replaces
compose under the accepted owner amendment.

Full local evidence: 276 tests, no failures/errors/skips. See
[workflow record](../../ai-context/06_TESTING_EVIDENCE/2026-10-30_authentication_workflows.md).
P05 as a whole remains open: RBAC/catalogue, actor/auth audit wiring, privileged
onboarding, async propagation, full I1–I9, changed-line coverage and actual dev
curl/browser journeys still require their own evidence. No hosted CI or publication.
