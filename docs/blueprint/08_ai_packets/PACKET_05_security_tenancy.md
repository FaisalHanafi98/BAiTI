# P05 — Security & Tenancy Core (XL — execute as five sequential slices)

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
- Startup `PermissionSyncService`: idempotently upserts missing catalog permissions + default grants per tenant (04 §2 rationale).
Tests: authz-matrix harness (06 §5.3) with fixture table seeded from 02 §8; endpoint-enumeration assertion armed (any future unclassified endpoint fails).

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
