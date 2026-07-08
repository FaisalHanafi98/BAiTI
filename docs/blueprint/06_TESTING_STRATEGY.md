# BAiTI — Testing Strategy

> **Blueprint document 6 of 9** | Produced: 2026-07-07
> TDD is non-negotiable (CON-5): every behaviour starts as a failing test. This document defines what kinds of tests exist, what they must prove, and which of them are **release gates**. Evidence rules at §10 apply to every claim of "done".

---

## 1. Philosophy & Pyramid

Prefer **vertical-slice integration tests against real PostgreSQL** (Testcontainers) as the backbone — RLS, constraints, and transactions are the system's riskiest parts and none of them exist on mocks or H2. Unit tests cover pure logic (state machines, balance math, validators). E2E is thin and late.

```
        Playwright E2E (post-MVP)        ~ dozens   · critical journeys only
      Frontend component (Vitest+RTL)    ~ hundreds · behaviour, not markup
    Backend integration (Testcontainers) ~ hundreds · slice: HTTP→service→JPA→PG(RLS)
  Backend unit (JUnit5+Mockito)          ~ hundreds · domain rules, mappers, guards
  + ArchUnit (architecture as tests) · + contract (OpenAPI diff) · + migration test
```

Speed budget: full backend `mvn verify` ≤ 5 min local (one shared PG container per module via singleton pattern); frontend suite ≤ 1 min. A slow suite is a defect — it kills the TDD loop.

## 2. Backend Unit Tests

Scope: state-machine transition guards (complaint/maintenance/booking/bill), bill-balance arithmetic (BigDecimal edges: exact cover, overpay reject, partials), permission-evaluator logic, DTO validation annotations, approval-code generation, token-family logic. Mockito for collaborators; **never mock repositories in slice tests** — that's what §3 is for. Naming: `method_condition_expectedOutcome()`.

## 3. Backend Integration Tests (Testcontainers — the backbone)

Every feature packet ships slice tests: `MockMvc` (or `WebTestClient`) → real Spring context → real PG16 container → Flyway-migrated schema.

Mandatory baseline suites (referenced by packets):

**3.1 Migration test** — empty PG16 → all migrations apply → assert: 26 relations, RLS enabled+forced on all 24 tenant tables, policies present, `baiti_app` grants correct (and UPDATE/DELETE absent on `payments`/`activity_logs`).

**3.2 Repository smoke per aggregate** — CRUD + unique-constraint violations surface as expected exceptions.

**3.3 Cross-tenant isolation suite** — see §5. Runs in every CI build from Packet 05 onward.

**3.4 Auth suite** — login happy/wrong-password/inactive-user/suspended-tenant (all generic 401); refresh rotation; **reuse of rotated token revokes family**; logout revokes; rate limit returns 429.

**3.5 Authorization matrix** — see §5.3.

## 4. Frontend Tests

Vitest + RTL: forms validate per Zod schema (error text visible, `role="alert"`), permission-gated UI (`<Can>`) hides/shows, auth store transitions, axios interceptor refresh-on-401 flow (msw-mocked), route guards redirect. MSW (mock service worker) fixtures generated from the OpenAPI examples so FE tests can't drift from the contract. No snapshot-only tests — assert behaviour.

## 5. The Tenant-Isolation Test Suite (RELEASE GATE — every build, forever)

The suite creates **two tenants (A, B)** through the real onboarding service, then proves, at minimum:

| # | Proof | Layer proven |
|---|---|---|
| I-1 | A's user listing units sees only A's units (B seeded with distinctive rows) | full stack |
| I-2 | A requests B's resource by known UUID ⇒ **404** (not 403, not 200) | app + RLS |
| I-3 | A attempts write with B's tenant-scoped FK (e.g. payment against B's bill) ⇒ 404/409, no row written | WITH CHECK |
| I-4 | JWT claim tenant ≠ resolved slug ⇒ 401 before controller | layer 1 |
| I-5 | Raw `JdbcTemplate` query with **no** tenant context ⇒ 0 rows (fail closed) | RLS alone |
| I-6 | Raw insert without context ⇒ rejected | RLS WITH CHECK alone |
| I-7 | `current_user` in runtime pool = `baiti_app` and `rolbypassrls = false`; owner-bypass regression canary: connect as `baiti_app`, `SET app.current_tenant = A`, select B's rows ⇒ 0 | §6.2 fix stays fixed |
| I-8 | `@Async` listener writes (audit/notification) land under the *originating* tenant | event tenant propagation |
| I-9 | Tenant onboarding rollback: force step-4 failure ⇒ no tenants row, no partial seeds | FR-TEN-02 |

Failure of any I-test **blocks merge and blocks release** — no exceptions, no skips (root SOP §4.4).

**5.3 Authorization matrix test**: parametrised over the access-control grid (02 §8): for every (endpoint × role) pair, expected 2xx/403/404. The grid lives as a test fixture table so adding an endpoint without classifying it fails the test (`unmapped endpoint` assertion via endpoint enumeration from the OpenAPI doc). Own-scope (`*_OWN`) endpoints additionally prove: Resident 1 cannot read Resident 2's records *within the same tenant*.

## 6. System & UAT Testing

**System testing** (staging, per milestone): journey scripts J1–J5 (02 §5) executed end-to-end against a deployed build with two seeded tenants; evidence captured per the existing templates in `docs/ai-context/06_TESTING_EVIDENCE/browser_verification.md` (step/expected/actual tables — those templates are adopted as-is, including the Phase-5 and Phase-8 flow tables already defined there).

**UAT plan** (pre-v1.0, with Anep + one friendly MC persona if available):
- Coverage: per role × per module CRUD + each state machine walked to every terminal state + billing lifecycle with partial payment + cross-tenant spot checks from the UI.
- Entry criteria: all I-tests green in CI, system-test evidence current, seed data reset script working.
- Exit criteria: zero SEV-1/2 open; SEV-3s triaged into backlog; sign-off note recorded in `docs/ai-context/06_TESTING_EVIDENCE/`.
- UAT defects follow the bug template in `docs/ai-context/03_BUG_HISTORY/unresolved_bugs.md`.

## 7. Performance & Stress Testing (milestone M5, pre-go-live)

Tool: **k6** against staging with seeded volume (2 tenants × 500 units × 12 months bills ≈ 12 k bills + payments).
- Load: 50 VU mixed read/write for 10 min ⇒ assert p95 < 200 ms reads / < 400 ms writes, 0 errors.
- Stress: ramp to 200 VU ⇒ find the knee; document it; no crashes/leaks (heap stable).
- Soak: 20 VU for 2 h ⇒ no connection-pool exhaustion, no memory creep.
- N+1 audit: Hibernate statistics on list endpoints during load — query count per request must be O(1), not O(rows).
Results filed in testing-evidence; regressions vs the recorded baseline > 20 % fail the release checklist.

## 8. Security Testing

| Check | When |
|---|---|
| OWASP Dependency-Check (backend) + `npm audit` (frontend) fail on high/critical | every CI run |
| Isolation suite (§5) — the tenant-boundary pen-test in code | every CI run |
| AuthZ matrix (§5.3) | every CI run |
| Secret scan (git-secrets/gitleaks pre-commit + CI) | every commit |
| Manual OWASP Top-10 pass with the `security-review` checklist: injection (parameterised only), broken auth (token flows §3.4), sensitive-data exposure (log redaction spot check), broken access control (matrix + IDOR attempts with foreign UUIDs), security misconfig (actuator exposure, CORS allowlist, headers), SSRF/file upload (type/size/path tests) | before each milestone release |
| JWT abuse cases: expired, wrong signature, `alg:none`, tampered claims, replayed rotated refresh | Packet 05 tests, kept forever |

## 9. Regression & Gates Summary

| Gate | Enforced where |
|---|---|
| All tests green, zero skips without written reason | CI, every PR |
| Backend ≥ 70 % / frontend ≥ 60 % changed-line coverage | CI (JaCoCo / Vitest) |
| Migration test + isolation suite + authz matrix | CI, every PR |
| ArchUnit architecture rules | CI, every PR |
| OpenAPI breaking-change diff | CI, every PR |
| Lint/format (Spotless, ESLint) | CI + pre-commit |
| k6 baseline regression < 20 % | release checklist |
| Restore-from-backup rehearsal | go-live gate |

## 10. Acceptance-Criteria & Evidence Conventions

- Every FR implemented gets Given/When/Then criteria in its packet **before** code; the failing test is written from the criteria verbatim.
- **Verification-before-completion** (root skill, localised here): no packet, task, or phase is reported complete without the actual command output (test run, curl, or screenshot) captured — to the PR description and, for milestones, to `docs/ai-context/06_TESTING_EVIDENCE/`. "It should work" is a review-rejection phrase.
- Bug fixes: reproduce with a failing test first (systematic-debugging skill); the test stays as regression armor.
- Flaky test policy: quarantine ≤ 7 days with an issue; a quarantined isolation/authz test blocks release anyway.
