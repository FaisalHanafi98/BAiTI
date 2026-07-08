# BAiTI — Quality Attributes Plan

> **Blueprint document 5 of 9** | Produced: 2026-07-07
> For each attribute: the target, how the architecture achieves it, and how it is *measured* — an unmeasured quality target is a wish. NFR IDs refer to `01_SRS.md §5`.

---

## 1. Performance (NFR-PERF)

| Budget | Target | Measured by |
|---|---|---|
| API p95 (CRUD reads) | < 200 ms | Micrometer timers per endpoint; Prometheus |
| API p95 (writes incl. side-effects) | < 400 ms | same |
| DB query | < 50 ms each; no N+1 | `show-sql` review in dev; Hibernate statistics in perf test |
| Frontend FCP / TTI | < 3 s / < 5 s on 10 Mbps | Lighthouse CI budget per PR (post-frontend-scaffold) |
| Bundle | < 250 KB gz initial route | `vite build` size report in CI |

How the design gets there: pagination capped at 100 everywhere; FK+tenant-leading indexes (04 §1.10); `JOIN FETCH` or entity-graph on known list screens (complaint list + category + resident) to kill N+1; async side-effects off the request path (02 §6.2); code-split routes; TanStack Query caching with 60 s staleTime default. **Rule: no speculative optimization** — any cache/denormalization beyond this list needs a profiler trace attached to its PR.

## 2. Scalability (NFR-PERF/ASM-3)

Designed ceiling v1.0: 50 tenants / 20 k units / ~100 concurrent users — single app node + single PG comfortably. Growth levers in order (03 §11): replicas of the stateless app → PG read replica → partition `activity_logs`+`notifications` → whale-tenant DB extraction at the `TenantAwareDataSource` seam. Each lever is documented, none pre-built. Load test at M5 (06_TESTING §7) validates the ceiling before go-live.

## 3. Security (NFR-SEC) — threat model (STRIDE × main assets)

Assets: A1 tenant domain data · A2 credentials/tokens · A3 financial records · A4 uploaded files · A5 audit log · A6 platform (super-admin) plane.

| Threat | Vector | Mitigation (layer) |
|---|---|---|
| **S**poofing | Forged JWT | HS256 ≥256-bit secret, env-only; 15-min TTL; signature verified every request |
| | Stolen refresh token replay | rotation + family reuse-detection (03 §7.2); token stored hashed |
| | Login brute force | bucket4j per-IP + per-(tenant,email) (FR-AUTH-06); generic 401s (FR-AUTH-05) |
| **T**ampering | Cross-tenant write | RLS `WITH CHECK` (04 §5) + Hibernate `@TenantId` stamp + JWT claim match |
| | Payment/bill mutation | payments append-only (grants revoked at DB); state machine guards; `FOR UPDATE` balance invariant |
| | Malicious upload (path traversal, disguised type) | StorageService generates keys; extension+MIME allowlist; 5 MB cap; never user-controlled paths/content-type |
| **R**epudiation | "I never approved that" | activity_logs append-only w/ user, IP, JSONB detail (FR-AUD-01/02); complaint_updates trail |
| **I**nformation disclosure | Cross-tenant read | the three-layer core: JWT → @TenantId → RLS FORCE + NOBYPASSRLS app role |
| | Existence oracle | foreign-tenant resource ⇒ 404 always (NFR-SEC-04); login/tenant errors generic |
| | Secrets in repo/logs | `.env.example` only; secret-scanning pre-commit hook; log redaction rules (03 §9); JWT never logged |
| | PII leak (PDPA) | synthetic seeds/fixtures only; no personal data in logs; Guard sees name+unit only (SRS §8.5) |
| **D**enial of service | Auth-endpoint floods | rate limits; PaaS-level protections; pool sized & monitored (Hikari metrics) |
| | Expensive queries | pagination caps; statement timeout (`statement_timeout=5s` on app role) |
| **E**levation | Resident → staff permissions | method-level `@PreAuthorize` from the matrix (02 §8) — UI hiding is never the control |
| | Tenant admin → platform | separate `super_admins` principal + separate filter chain (03 §7.5); tenant RBAC cannot express platform perms |
| | SQL injection → RLS bypass attempt | JPA parameter binding everywhere; even raw SQL stays inside RLS (`FORCE`, NOBYPASSRLS) |

**OWASP Top-10 mapping** lives with the checklist in 06_TESTING §8 (security test plan). **Secrets management**: env vars only; shared password manager for the two collaborators; rotation runbook (JWT secret rotation = deliberate global logout). **Dependency hygiene**: OWASP Dependency-Check + `npm audit` fail CI at high/critical; Dependabot weekly.

## 4. Reliability (NFR-REL)

- **Atomicity**: tenant onboarding, bill+payment, booking approval — single `@Transactional` boundaries; the failure test for each is part of its packet's DoD.
- **Idempotency**: tenant creation by slug (FR-TEN-02); dev seeding; safe retries on GETs; POSTs return 409 on duplicate natural keys rather than double-writing.
- **Side-effect failure isolation**: audit/notification listeners never fail the business op (FR-AUD-03) — but count their failures in a metric (`baiti_listener_failures_total`) so silence ≠ health.
- **Backups/DR**: nightly dump, 30-day retention, RPO 24 h / RTO 4 h, rehearsed restore as a go-live gate (03 §13).
- **Graceful degradation**: file-storage outage blocks uploads only (bills still recordable without proof, flag for later attach).

## 5. Availability (NFR-AVL)

99.5 %/month target v1.0 (allows ~3.6 h downtime — honest for a single node + deploys). `/actuator/health` with DB liveness for platform probes; graceful shutdown (in-flight requests drain ≤ 30 s); deploys are stop-start at v1.0 (blue/green only post-v1.0 — documented gap, accepted). Error budget reviewed monthly; two consecutive misses trigger the availability backlog (replicas, managed PG HA).

## 6. Maintainability (NFR-MNT)

- Coverage gates in CI: backend ≥ 70 % changed-lines (JaCoCo), frontend ≥ 60 % (Vitest).
- Architecture conformance as tests: the ArchUnit suite (03 §3 rules 1–5) — boundary violations fail the build, not code review.
- Style: Spotless (google-java-format) + ESLint/Prettier — machine-enforced, never debated in review.
- Contract: OpenAPI artifact diffed per PR; generated FE types (NFR-MNT-02).
- Docs-as-code: ADRs for every §15 (03) decision; `docs/ai-context/` update rules apply to every session (archive README).
- Conventional commits `type(scope): …`; user reviews every message (CON-4).

## 7. Accessibility (NFR-ACC-01) — WCAG 2.1 AA

PR checklist (enforced on every UI packet): 4.5:1 text contrast (Tailwind palette pre-verified once, tokens only thereafter) · every input labelled + error text linked via `aria-describedby` · full keyboard path for every journey J1–J5 · visible focus ring (never `outline-none` without replacement) · 44×44 px touch targets · Headless UI for menus/dialogs/tabs (correct ARIA + focus trap for free) · form errors announced (`role="alert"`) · tables with proper `th/scope` · no color-only status signalling (status chips = color + label). Axe automated scan wired into Playwright post-MVP; manual keyboard pass is part of UAT.

## 8. Internationalisation (NFR-I18N-01)

react-i18next from the first screen; `en` bundle is the only shipped language; **zero hard-coded user-facing strings** (ESLint rule `no-literal-strings` on JSX, allowlist for technical tokens). Backend returns error *codes* (RFC 7807 `type`), frontend owns human wording — so BM (D5) is a translation file, not a refactor. All timestamps UTC in DB/API; `Asia/Kuala_Lumpur` formatting client-side; currency formatting via `Intl.NumberFormat('ms-MY', {currency:'MYR'})`.

## 9. Monitoring & Observability (NFR-OBS-01)

**Logs**: structured JSON; MDC `requestId`/`tenantId`/`userId`; retention 30 d. **Metrics** (Micrometer → Prometheus): http server timers (p50/95/99 per route), error rate, Hikari pool, JVM, `baiti_login_failures_total{tenant}`, `baiti_listener_failures_total{listener}`, `baiti_rls_missing_context_total` (custom counter wherever fail-closed triggers — a nonzero value is a bug alarm). **Health**: liveness (app) + readiness (DB ping). **Alert baselines** (platform-level at v1.0): error rate > 2 % for 5 min · p95 > 500 ms for 15 min · health down > 2 min · disk > 80 %. **Drill**: "user complaint → requestId → full request trace in < 2 min" rehearsed at M5 (runbook).

## 10. Attribute Trade-offs Made Explicit

| Tension | Ruling |
|---|---|
| Security vs DX (RLS fail-closed can hide bugs as "no rows") | Keep fail-closed; compensate with `baiti_rls_missing_context_total` metric + dev-profile WARN log when context is empty on a tenant path |
| Availability vs cost | 99.5 % on one node accepted at v1.0; money goes to correctness (tests) first |
| Performance vs simplicity | No caches/brokers until a profiler says so (03 §10) |
| Velocity vs coverage gates | Gates apply to changed lines, not repo-wide history — keeps early phases honest without freezing them |
| PDF fidelity vs correctness | Correctness wins; every deviation registered (04 §9) |
