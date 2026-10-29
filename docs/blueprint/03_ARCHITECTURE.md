# BAiTI — Architecture Design

> **Blueprint document 3 of 9** | Produced: 2026-07-07
> Every decision states its *why*. Sections 6 (tenancy) and 7 (auth) are normative — implementation agents must not improvise there. Corrections to the 2026-04-08 plan are marked **[FIX]**.

---

## 1. Architecture Style

**Modular monolith in a monorepo.** One Spring Boot deployable, packages-by-feature with enforced boundaries; one React SPA; one PostgreSQL cluster shared by all tenants (shared-schema, row-isolated).

**Why**: two part-time developers; a monolith maximises feature velocity and keeps transactions simple (tenant onboarding, bill+payment atomicity). Package-by-feature with ArchUnit-enforced interfaces keeps a future extraction path open without paying microservice tax now. Shared-schema multi-tenancy is the cheapest isolation model that still gets database-enforced guarantees via RLS; schema-per-tenant was rejected (migration fan-out, connection-pool fragmentation at 50 tenants).

```
baiti/  (monorepo)
├── backend/        Spring Boot 3.x · Java 17 · Maven · package root my.baiti
├── frontend/       React 18 · TypeScript · Vite · Tailwind
├── infra/          docker-compose (postgres:16 + pgadmin) · Dockerfiles
├── docs/           blueprint/ · adr/ · ai-context/ · audits/ · specs/ · plans/
└── .github/workflows/
```

## 2. Technology Stack (locked — CON-1)

| Concern | Choice | One-line why (full rationale: `meeting/02_STACK_RATIONALE.md`) |
|---|---|---|
| Backend | Spring Boot 3.x / Java 17 / Maven | Type safety across 25 relational tables; Spring Security maturity; hireability |
| DB | PostgreSQL 16 | **RLS is the reason** — DB-enforced tenant isolation; NUMERIC for money; UUID native |
| Migrations | Flyway | SQL-first, append-only history, Testcontainers-verifiable |
| ORM | Spring Data JPA / Hibernate 6 | `@TenantId` discriminator support (layer 2 of isolation) |
| AuthN/Z | Spring Security + JWT (HS256) + bcrypt(12) | Standard, method-level `@PreAuthorize` |
| Frontend | React 18 + TS + Vite | React Native path; OpenAPI-generated types kill contract drift |
| Server state | TanStack Query | cache/retry/invalidations for free |
| Client state | Zustand | auth/session + UI state, minimal boilerplate |
| Forms | React Hook Form + Zod | schema = validation = TS type |
| Styling | Tailwind + Headless UI | speed + accessible primitives |
| API contract | springdoc-openapi → `openapi-typescript` | backend is the single source of API truth |
| Tests | JUnit 5 + Mockito + Testcontainers · Vitest + RTL · Playwright (post-MVP) | real-Postgres integration tests; RLS is untestable on H2 |
| CI/CD | GitHub Actions · Docker | free tier, branch protection |

**Deliberately excluded** (revisit only with a written ADR): microservices, Kubernetes, GraphQL, Redux, message brokers, Firebase, MongoDB, Next.js/SSR.

## 3. Backend Layering

Per feature package: `Controller → Service → Repository → Entity` + `dto/` records. Idiom reference (canonical code shapes): `meeting/03_SPRINGBOOT_REFRESHER.md`.

Rules (ArchUnit tests in CI — Packet 03):
1. Controllers never touch repositories; controllers speak DTOs only, never entities.
2. Cross-package calls go through `*Service` interfaces; repositories are package-private.
3. Every tenant `@Entity` inherits tenant/audit fields from `TenantScopedBaseEntity`. Ordinary UUID-id entities extend its `TenantAwareBaseEntity` subclass. `RolePermission` uses the existing composite (role_id, permission_id) key and extends `TenantScopedBaseEntity` directly. `Tenant` and `SuperAdmin` remain global exceptions. This 29 October mapping repair adds no database columns.
4. Only `my.baiti.security` may import JWT libraries; only `my.baiti.tenancy` may write `TenantContext`.
5. Entity timestamps use `Instant`/`OffsetDateTime` (UTC), never `LocalDateTime`; legitimate calendar values such as billing month/due date remain `LocalDate`. Formatting is a frontend concern (NFR-I18N-01).

`common` package: `TenantAwareBaseEntity` (id UUID, tenantId, createdAt, updatedAt, createdBy, updatedBy — JPA auditing), `ApiError` (RFC 7807 shape), global `@RestControllerAdvice`, pagination defaults.

## 4. API Strategy

- **REST/JSON under `/api/**`**; resource-per-table with workflow verbs as sub-resources (`POST /api/complaints/{id}/updates`, `POST /api/bookings/{id}/approval`). Kebab-case paths, camelCase JSON.
- **Errors**: RFC 7807 `application/problem+json` — `{type,title,status,detail,instance,errors:[{field,message}]}`. Validation ⇒ 400; missing permission ⇒ 403; wrong/foreign tenant resource ⇒ **404 always** (NFR-SEC-04); auth failures ⇒ 401; rate limit ⇒ 429; state-machine violations ⇒ 409.
- **Pagination**: Spring `Pageable` (`page`,`size`≤100,`sort`); responses wrap `{content,totalElements,totalPages,page,size}`.
- **Versioning**: none in the path for v1.0; the OpenAPI artifact is diffed in CI — breaking changes fail the build; `/api/v2` reserved as the escape hatch. Why: two clients we own, contract-generated types make drift loud already.
- **OpenAPI**: springdoc generates `/v3/api-docs`; a CI job commits the frozen `openapi.json` and regenerates `frontend/src/api/types.gen.ts` (hand-editing that file is forbidden — NFR-MNT-02).
- **IDs**: UUIDv4 everywhere; never sequential (business-size leak).

## 5. Frontend Architecture

Structure per `meeting/04_REACT_REFRESHER.md` §11 (canonical): `api/` (axios client + generated types), `stores/` (Zustand: auth, UI), `pages/` (route-level, code-split via `React.lazy`), `components/ui/` + per-domain components, `hooks/`, `lib/`.

- Server state only in TanStack Query (raw `useEffect` fetching is a review-rejection); client state only in Zustand; form state only in RHF.
- Route guard: `ProtectedRoute` + permission-aware `<Can permission="...">` helper reading the JWT claims from the auth store — UI hiding is convenience; the API remains the enforcement point (FR-RBAC-03).
- i18n: `react-i18next` initialised at scaffold time with `en` bundle; all copy through `t()` from the first screen (NFR-I18N-01).
- Accessibility: Headless UI primitives + the WCAG 2.1 AA checklist as PR gate (NFR-ACC-01).

## 6. Multi-Tenancy — Normative Design

### 6.1 Three independent layers

| Layer | Mechanism | Fails how |
|---|---|---|
| 1 · Identity | `tenantId` claim inside signed JWT; `TenantContextFilter` rejects (401) any mismatch between claim and slug/subdomain resolution | Forged/mismatched context never reaches services |
| 2 · Application | Hibernate 6 `@TenantId` on `TenantAwareBaseEntity.tenantId` + `CurrentTenantIdentifierResolver` reading `TenantContext` (ThreadLocal) — every JPA query auto-filtered, every insert auto-stamped | A forgotten WHERE clause is impossible for JPA paths |
| 3 · Database | PostgreSQL RLS policy on every domain table: `USING (tenant_id = current_setting('app.current_tenant', true)::uuid)` | Native SQL, JDBC, or a layer-2 bug still cannot cross tenants |

### 6.2 [FIX] Two database roles — RLS does not bind table owners

PostgreSQL RLS **does not apply to the table's owner** (nor superusers) unless forced. Therefore:

```sql
-- provisioned by infra (docker-compose init / prod bootstrap), not by Flyway itself
CREATE ROLE baiti_migrator LOGIN PASSWORD :mig_pw;  -- owns schema, runs Flyway
CREATE ROLE baiti_app      LOGIN PASSWORD :app_pw NOBYPASSRLS;  -- runtime datasource
GRANT USAGE ON SCHEMA public TO baiti_app;
-- per-table (in migrations): GRANT SELECT,INSERT,UPDATE,DELETE ... TO baiti_app;
ALTER TABLE <every domain table> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <every domain table> FORCE ROW LEVEL SECURITY;   -- belt for owner paths too
```

Spring uses **two datasources by role**: Flyway runs as `baiti_migrator`; the Hikari runtime pool connects as `baiti_app`. An integration test asserts `current_user = 'baiti_app'` and that RLS denies an unscoped query (06_TESTING §5.2). Without this split, every RLS promise in the older planning docs was void.

### 6.3 [FIX] Propagating the tenant to the database — pool-safe

`SET LOCAL` is a no-op outside a transaction, so "an interceptor runs SET LOCAL" is not a design. Normative mechanism — **`TenantAwareDataSource`** (delegating wrapper around Hikari):

- `getConnection()` → if `TenantContext` holds a tenant: `SELECT set_config('app.current_tenant', '<uuid>', false)`; if empty: `RESET app.current_tenant`.
- Proxy `close()` → `RESET app.current_tenant` before returning the connection to the pool (no cross-request bleed).
- RLS policies read with `current_setting('app.current_tenant', true)` → returns NULL when unset → predicate false → **zero rows, fail closed** (FR-TEN-05).

`TenantContext` is set once by `TenantContextFilter` (after JWT auth) and cleared in `finally`. `@Async` side-effect listeners receive the tenantId explicitly inside the event payload — ThreadLocals do not cross thread pools.

### 6.4 Tenant resolution

`TenantResolver` interface, profile-selected: `HeaderTenantResolver` (`X-Tenant-Slug`, dev/test) and `SubdomainTenantResolver` (`{slug}.baiti.app`, prod — final domain TBD/D3). Resolution feeds login (which tenant's users table to check) and the layer-1 mismatch check on every authenticated call.

### 6.5 Global (non-tenant) data and the super-admin path

`tenants`, `super_admins`, `flyway_schema_history`, and role/permission **templates** are global tables — no tenant_id, no tenant-isolation policy. Super-admin endpoints (`/api/admin/**`) run with empty `TenantContext`; they may only touch global tables (ArchUnit: `my.baiti.tenancy.admin` may not import domain repositories). Tenant onboarding (UC-T1) writes tenant-scoped rows *before a tenant user exists*: the onboarding service runs its transaction on the **migrator-role datasource** deliberately and is the single annotated, code-reviewed exception (`@UsesPrivilegedDataSource`).

## 7. Security Architecture

### 7.1 AuthN flow
Stateless JWT per `meeting/03_SPRINGBOOT_REFRESHER.md` §6: CSRF off, sessions STATELESS, `JwtAuthenticationFilter` before username/password filter. Access token 15 min HS256 (`userId`,`tenantId`,`roles`,`permissions`,`jti`); secret ≥ 256-bit from env, rotation procedure documented in runbook.

### 7.2 Refresh tokens — rotation with reuse detection
`refresh_tokens` table (hashed token, family_id, user, tenant, expires_at, rotated_at, revoked_at). Refresh ⇒ old row marked rotated, new row same family. **Presenting a rotated token revokes the family** (stolen-token replay defence, FR-AUTH-03). Logout/deactivation revokes all user rows. Cleanup job purges expired rows.

### 7.3 AuthZ
Role → permissions resolved at login into JWT claims; `@PreAuthorize("hasAuthority('BILL_MANAGE')")` per endpoint from the matrix (02_SYSTEM_ANALYSIS §8). Own-record scoping (`*_OWN`) is a mandatory service-layer predicate (resident ↔ resource join) — tested per endpoint. Changing a role's permissions takes effect on next token issue (≤15 min lag accepted at v1.0; documented).

### 7.4 Token storage in the browser **[decision]**
Access token: **memory only** (non-persisted Zustand). Refresh token: **httpOnly Secure SameSite=Strict cookie** scoped to `/api/auth/refresh`; page load performs silent refresh to bootstrap. Why: localStorage tokens are one XSS from account takeover; this design caps exposure at 15 min. The refresh endpoint also accepts the token in the body (future mobile). CORS: explicit origin allowlist + credentials, dev `http://localhost:5173`.

### 7.5 Super-admin isolation
Separate `super_admins` table (D6): no tenant_id, distinct principal type, `/api/admin/**` security chain requires it. A tenant Admin can never escalate platform-wide; tenant user compromise cannot reach cross-tenant surface.

### 7.6 Boundary hardening
Bean Validation on every DTO; Zod mirrors client-side. File uploads: size/type allowlist (SRS §8.6), stored via `StorageService` with generated names — never user-supplied paths, never served with user-controlled content-type. Rate limiting: bucket4j on `/api/auth/**` (per IP + per tenant/email). Security headers via Spring defaults + CSP for the SPA host. Actuator: `health` public (probe), everything else authenticated.

## 8. Configuration Management

Profiles: `dev` (compose DB, header resolver, seeds V900+), `test` (Testcontainers), `prod` (env-only secrets, subdomain resolver). `ddl-auto: validate` always — Flyway owns schema.

**Environment variable inventory** (contract for `.env.example`, Packet 02):

| Variable | Purpose |
|---|---|
| `DB_URL` | JDBC URL |
| `DB_APP_USER` / `DB_APP_PASSWORD` | runtime pool (`baiti_app`) |
| `DB_MIGRATOR_USER` / `DB_MIGRATOR_PASSWORD` | Flyway (`baiti_migrator`) |
| `JWT_SECRET` | ≥256-bit; never defaulted in prod |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `7d` defaults |
| `CORS_ALLOWED_ORIGINS` | comma list |
| `STORAGE_ROOT` (dev) / `S3_*` (prod, later) | StorageService backend |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | default 5 |
| `APP_BASE_DOMAIN` | subdomain resolver (prod) |
| `VITE_API_URL` / `VITE_TENANT_SLUG` | frontend dev |

## 9. Logging & Observability

Structured JSON (Logback encoder) with MDC: `requestId` (generated per request, echoed as `X-Request-Id`), `tenantId`, `userId`. Never log: passwords, tokens, full personal data (NFR-CMP-01). Levels: business events INFO, validation noise DEBUG, listener failures ERROR. Actuator + Micrometer → Prometheus registry; dashboards: p95 latency per endpoint, error rate, Hikari pool saturation, login failures per tenant (abuse signal). Correlation of a user complaint → requestId → full trace must take < 2 min (runbook drill).

## 10. Caching & Async

**v1.0: no cache layers.** Correctness first; the data is small and RLS makes cross-tenant cache-key mistakes dangerous. If profiling later shows hot spots, first candidate is a Caffeine cache on role→permission template lookups — keyed by tenantId, short TTL, with an ADR.
**Async**: in-process Spring events + `@Async` listeners (audit, notifications) with a bounded executor; failures logged, never propagated (FR-AUD-03). No broker at this scale; if delivery guarantees ever matter (email/SMS), introduce a transactional outbox table first, broker last.

## 11. Scalability Path (documented, not built)

1. v1.0: single app instance + single Postgres. Stateless app ⇒ horizontal scale is a replica-count change behind the platform LB (JWT, no sticky sessions).
2. Read pressure → Postgres read replica for dashboards/reports.
3. `activity_logs`/`notifications` growth → monthly partitioning (04_DATABASE §8) + retention jobs.
4. Big tenant skew → RLS design already permits per-tenant `pg_stat` inspection; extraction to dedicated DB is a data migration, not a code rewrite (connection routing by tenant slug at the `TenantAwareDataSource` seam).

## 12. Deployment Strategy & Target Matrix (decision D3 pending — architecture is target-agnostic)

Artifacts: two images (`backend`: JRE-17 multi-stage; `frontend`: static build served by nginx or the platform CDN) + managed/containerised Postgres 16.

| Option | Monthly ≈ | Managed PG | SG region | Fit |
|---|---|---|---|---|
| **Fly.io** (recommended) | ~US$5–10 | via fly-postgres/Neon | **Yes (sin)** | Best latency to MY; Dockerfile-native |
| Railway | ~US$5–20 | Yes | US/EU only today | Best DX for staging |
| Render | free–US$14 | Yes | SG absent; closest US-West | Free-tier demos |
| VPS (Hetzner/DO) + compose | ~US$6–12 | self-managed | DO has SGP | Cheapest; you own backups/patching |

Pipeline (Plan §7.3–7.5 adopted): CI on PR (backend: `mvn verify` + Testcontainers + OWASP-check + JaCoCo gate; frontend: lint/typecheck/test/build/audit) → merge to `dev` auto-deploys dev env → staging by manual approval → prod by manual approval + checklist + retained previous image tag. **Rollback**: redeploy previous tag; DB is forward-fix only (no Flyway undo) with pre-deploy backup; deploy checklist includes "does this migration break the previous app version?" (expand-migrate-contract discipline for column changes).

## 13. Disaster Recovery

Nightly `pg_dump` (custom format) to off-site object storage, 30-day retention; RPO ≤ 24 h / RTO ≤ 4 h (NFR-REL-02). **Restore rehearsal is a go-live gate**: restore latest dump into a fresh container, run smoke suite, record evidence in `docs/ai-context/06_TESTING_EVIDENCE/`. Post-v1.0: WAL archiving/PITR shrinks RPO to minutes. Secrets escrow: JWT secret + DB passwords in the collaborators' shared password manager; losing the JWT secret = global logout (acceptable), losing DB creds = platform-console reset (documented in runbook).

## 14. Future Extensibility (kept-open doors)

| Future | Kept open by |
|---|---|
| React Native app | JWT + REST + generated types; auth/api/hooks layers port as-is; refresh-in-body path (§7.4) |
| Payment gateway | Immutable payments + method FK + receipt_no already gateway-shaped; add `finance.gateway` sub-package + webhook endpoint |
| Email/SMS | `NotificationChannel` interface; outbox pattern noted (§10) |
| BM language | react-i18next from day 1; add `ms` bundle |
| Tenant plan tiers | `tenants` table takes a `plan` column later; no code assumes "all features for all tenants" — permission templates are per-tenant already |
| Extraction to services | package-by-feature + interface-only cross-calls + ArchUnit-verified boundaries |

## 15. ADR Index (to be written as short records during bootstrap — Packet 01)

| ADR | Decision captured | Source |
|---|---|---|
| 0001 | Naming: BAiTI, folder/repo/package/class | launch prompt + 00_INVENTORY §3.5 |
| 0002 | Monolith, monorepo, package-by-feature | §1 |
| 0003 | Shared-schema multi-tenancy; three layers; dual DB roles; FORCE RLS | §6 |
| 0004 | JWT + rotating refresh with reuse detection; browser token storage | §7 |
| 0005 | Schema corrections from PDF (typos, payments FK, count=25) | 00_INVENTORY §3, 04_DATABASE |
| 0006 | Money = NUMERIC/BigDecimal, MYR-only + currency column | SRS D4 |
| 0007 | No cache, no broker at v1.0; outbox-first escalation rule | §10 |
| 0008 | Deployment target | D3 — pending |
