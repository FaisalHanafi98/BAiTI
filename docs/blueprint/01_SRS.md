# BAiTI — Software Requirements Specification (SRS)

> **Blueprint document 1 of 9** | Produced: 2026-07-07
> **System**: BAiTI — multi-tenant SaaS condominium/community management platform
> **Requirement IDs are stable.** Implementation packets, tests, and acceptance criteria reference them. Never renumber; deprecate with a strikethrough note instead.

---

## 1. Introduction

### 1.1 Purpose
BAiTI lets a management office run a residential community — property records, residents, billing, complaints, maintenance, facilities, communications, and visitor control — from one web application. One deployment serves many communities ("tenants"); each tenant's data is completely isolated from every other tenant's.

### 1.2 Intended audience
Implementation AI agents and the two collaborators (Faisal — backend lead; Anep — frontend lead, pending confirmation). This SRS is the contract that 02–08 of the blueprint elaborate.

### 1.3 Definitions

| Term | Meaning |
|---|---|
| **Tenant** | A customer community (one condominium/apartment complex). NOT a person renting a unit. |
| **Resident** | A person living in a unit. `resident_type` distinguishes **OWNER** from **TENANT_OCCUPANT** (the renting person — renamed from the PDF's "Tenant" value to avoid collision with the SaaS meaning; DB enum value remains `TENANT` for fidelity to the domain language, code must comment the distinction). |
| **Unit** | An apartment/lot within a block. |
| **Block** | A building/tower within the community. |
| **MC** | Management corporation / management office staff. |
| **RLS** | PostgreSQL Row-Level Security. |

### 1.4 Product scope (one paragraph)
v1.0 is a web application (desktop-first, responsive) with five must-have modules — auth/RBAC, property setup, residents, billing/payments, complaints/maintenance — on a foundation that is multi-tenant from the first migration. v1.x adds facilities, visitors, community communications, and notifications. Mobile (React Native) is explicitly post-v1.0. There is no payment-gateway integration in v1.0 (payments are recorded, not processed).

---

## 2. Business Requirements

| ID | Requirement | Rationale |
|---|---|---|
| BR-1 | One deployment shall serve N independent communities with zero data visibility across communities. | SaaS economics; the product is sellable to many condos without per-customer deployments. |
| BR-2 | The platform shall support the full money trail for community dues: bill issued → payment recorded → receipt referenced. | The finance module is the primary value driver for management offices. |
| BR-3 | Residents shall be able to raise and track complaints and maintenance requests without visiting the office. | Core resident-facing value; reduces MC workload. |
| BR-4 | The MC shall control who can do what via roles and granular permissions. | Offices have staff with different responsibilities (admin vs guard). |
| BR-5 | The system shall keep an audit trail of security-relevant actions. | Dispute resolution, accountability, PDPA expectations. |
| BR-6 | The codebase shall be a portfolio-grade demonstration of enterprise engineering (TDD, CI, layered security). | Explicit collaborator goal; shapes quality bars, not features. |
| BR-7 | Personal data shall be handled per Malaysian PDPA: no real personal data in the repository, seeds, or logs. | Legal compliance; root SOP §8.2. |
| BR-8 | The architecture shall not block a future React Native mobile client or future payment-gateway integration. | Stated product direction. |

---

## 3. User Classes (Personas)

| Persona | Description | Primary goals |
|---|---|---|
| **Super Admin** (platform) | Operates the BAiTI platform itself. Not a member of any tenant. | Onboard/suspend tenants; platform health. |
| **Admin** (tenant) | Head of the management office for one community. | Configure blocks/units/roles/users; oversee everything in their tenant. |
| **Manager** (tenant) | Operations staff. | Issue bills, record payments, work complaints and maintenance, approve bookings, post announcements. |
| **Resident** (tenant) | Owner or tenant-occupant of a unit. | View own bills, pay-and-prove, raise complaints/requests, book facilities, pre-register visitors, read announcements. |
| **Guard** (tenant) | Guardhouse staff. | Log walk-in visitors, verify pre-registrations, record entry/exit. |
| **System** | Scheduled/async processes. | Generate notifications, write audit logs, expire tokens. |

---

## 4. Functional Requirements

Priorities: **M** = Must (v1.0) · **S** = Should (v1.x) · **C** = Could (later). The M/S split follows the MVP proposal in `meeting/01_MEETING_BRIEF.md`; it is the blueprint baseline pending Anep ratification (decision D1).

### 4.1 Tenancy (FR-TEN) — foundation, Must

| ID | Requirement | Pri |
|---|---|---|
| FR-TEN-01 | Super Admin shall create a tenant with a unique slug, name, and status; creation shall atomically seed the tenant's default roles and permissions from templates and create the tenant's first Admin user. | M |
| FR-TEN-02 | Tenant creation shall be idempotent on slug: re-submitting the same slug shall not create a duplicate or a partially-seeded tenant. | M |
| FR-TEN-03 | Every domain record shall belong to exactly one tenant; no API response shall ever contain another tenant's data, enforced at JWT, application, and database (RLS) layers. | M |
| FR-TEN-04 | Requests shall resolve their tenant from the `X-Tenant-Slug` header (dev) or subdomain (prod); a request whose JWT tenant claim does not match the resolved tenant shall be rejected with 401. | M |
| FR-TEN-05 | A request with no resolvable tenant context shall fail closed (no data returned), including for raw SQL paths. | M |
| FR-TEN-06 | Super Admin shall list tenants and set tenant status (`ACTIVE`, `SUSPENDED`); users of a suspended tenant shall be unable to authenticate. | S |

### 4.2 Authentication (FR-AUTH) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-AUTH-01 | Users shall log in with tenant + email + password; success returns an access token (15 min, HS256; claims: `userId`, `tenantId`, `roles`, `permissions`) and a refresh token (7 days, rotating, stored server-side). | M |
| FR-AUTH-02 | Passwords shall be stored bcrypt-hashed (cost 12); never logged, never returned. | M |
| FR-AUTH-03 | `POST /api/auth/refresh` shall rotate the refresh token; a re-used (already-rotated) refresh token shall invalidate the whole token family and require re-login. | M |
| FR-AUTH-04 | `POST /api/auth/logout` shall revoke the presented refresh token. `GET /api/auth/me` shall return the authenticated user's profile, roles, and permissions. | M |
| FR-AUTH-05 | Login shall not reveal whether email or password was wrong, and shall not reveal tenant existence to unauthenticated callers. | M |
| FR-AUTH-06 | Failed logins shall be rate-limited per (tenant, email) and per IP. | M |
| FR-AUTH-07 | Password reset via emailed time-boxed token. | S |

### 4.3 RBAC (FR-RBAC) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-RBAC-01 | Each user shall have exactly one role (per the data model); each role maps to many permissions via `roles_permissions`. | M |
| FR-RBAC-02 | Default roles per tenant: Admin, Manager, Resident, Guard — seeded from templates at tenant creation; Admins may edit role-permission assignments within their tenant. | M |
| FR-RBAC-03 | Every mutating endpoint and every list/read endpoint shall be guarded by a named permission (e.g. `COMPLAINT_CREATE`, `BILL_READ_ALL`); enforcement is method-level (`@PreAuthorize`), not just UI hiding. | M |
| FR-RBAC-04 | Residents shall only read their own records (own bills, own complaints, own bookings, own visitors); "own-record" scoping is a service-layer rule on top of tenant scoping. | M |
| FR-RBAC-05 | Admin shall manage users in their tenant: create, deactivate, assign role. Deactivated users cannot authenticate and their refresh tokens are revoked. | M |

### 4.4 Property (FR-PROP) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-PROP-01 | Admin shall CRUD blocks (name unique per tenant, description). | M |
| FR-PROP-02 | Admin shall CRUD units within a block: `unit_no` (unique per block), `type`, `floor_no`, `status` (`OCCUPIED`/`VACANT`). | M |
| FR-PROP-03 | A block with units shall not be deletable (restrict); a unit with active residents shall not be deletable. | M |
| FR-PROP-04 | Unit status shall reflect residency automatically: assigning an active resident marks it `OCCUPIED`; removing the last one marks it `VACANT`. | S |

### 4.5 Residents & family (FR-RES) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-RES-01 | Admin/Manager shall register a resident: link to a user account (optional at creation, required for portal access), unit, name, phone, email, `resident_type` (`OWNER`/`TENANT`), `status` (`ACTIVE`/`MOVED_OUT`). | M |
| FR-RES-02 | A resident shall belong to exactly one unit at a time; a unit may house multiple residents. | M |
| FR-RES-03 | Residents/Admin shall CRUD family members under a resident: name, relationship, age, contact number. Family members are records, not login accounts. | M |
| FR-RES-04 | Moving a resident out shall retain history (soft status change, not delete) and revoke portal access if a user account is linked. | S |

### 4.6 Finance (FR-FIN) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-FIN-01 | Manager/Admin shall issue bills against a unit: billing month, amount (MYR, `NUMERIC(12,2)`), due date, status (`UNPAID`/`PAID`/`OVERDUE`/`CANCELLED`). One bill per (unit, billing_month, bill type) — duplicates rejected. | M |
| FR-FIN-02 | Manager shall record payments against a bill: paying resident, amount, payment method (FK to tenant's configured methods), receipt number, optional proof-of-payment file upload. | M |
| FR-FIN-03 | A bill shall become `PAID` when recorded payments cover its amount; partial payments leave it `UNPAID` with a computed balance. Overpayment shall be rejected. | M |
| FR-FIN-04 | Residents shall view their own units' bills and payment history; Managers/Admins view all in tenant. | M |
| FR-FIN-05 | Admin shall CRUD tenant payment methods (e.g. bank transfer, cash, cheque). | M |
| FR-FIN-06 | Amounts shall never be floats anywhere in the stack (DB `NUMERIC`, Java `BigDecimal`, TS strings/int cents at the boundary). | M |
| FR-FIN-07 | Bills unpaid past due date shall be flagged `OVERDUE` (scheduled job); Managers shall see an arrears view per unit/resident. | S |
| FR-FIN-08 | Payment-gateway integration (FPX/card). | C |

### 4.7 Complaints (FR-CMP) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-CMP-01 | Residents shall submit complaints: category (tenant-managed list), title, description, auto-captured unit and timestamp. | M |
| FR-CMP-02 | Complaint lifecycle: `OPEN → IN_PROGRESS → RESOLVED → CLOSED` (+ `REJECTED` from OPEN). Transitions restricted to Manager/Admin except that the submitting resident may close their own resolved complaint. | M |
| FR-CMP-03 | Managers/Admins shall append status updates with a message; the full update trail is visible to the complaint's resident and staff. | M |
| FR-CMP-04 | New complaints shall notify Manager-role users of that tenant (in-app notification record). | M |
| FR-CMP-05 | Admin shall CRUD complaint categories. | M |

### 4.8 Maintenance (FR-MNT) — Must

| ID | Requirement | Pri |
|---|---|---|
| FR-MNT-01 | Residents (or staff on their behalf) shall create maintenance requests: unit, description, priority (`LOW`/`MEDIUM`/`HIGH`/`URGENT`). | M |
| FR-MNT-02 | Request lifecycle: `PENDING → APPROVED → IN_PROGRESS → COMPLETED` (+ `REJECTED` from PENDING); transitions by Manager/Admin. | M |
| FR-MNT-03 | Managers shall view a queue filterable by status, priority, and block. | M |

### 4.9 Facilities (FR-FAC) — Should (v1.x)

| ID | Requirement | Pri |
|---|---|---|
| FR-FAC-01 | Admin shall CRUD facilities: name, location, capacity, rules text, status (`AVAILABLE`/`MAINTENANCE`/`CLOSED`). | S |
| FR-FAC-02 | Residents shall book a facility for a date + time slot; overlapping bookings for the same facility/slot shall be rejected at the database level (exclusion/unique constraint), not just in code. | S |
| FR-FAC-03 | Booking lifecycle: `PENDING → APPROVED/REJECTED → CANCELLED`; approval by Manager/Admin, recorded with approver. | S |

### 4.10 Community (FR-COM) — Should (v1.x)

| ID | Requirement | Pri |
|---|---|---|
| FR-COM-01 | Manager/Admin shall post announcements: title, content, optional attachment, target audience (`ALL`/`OWNERS`/`TENANTS`), posted timestamp. Residents see only announcements targeted at their resident type. | S |
| FR-COM-02 | Manager/Admin shall CRUD events: title, start/end datetime, location, description. Residents view a calendar/list. | S |

### 4.11 Visitors (FR-VIS) — Should (v1.x)

| ID | Requirement | Pri |
|---|---|---|
| FR-VIS-01 | Residents shall pre-register visitors: visitor name, visit date; system issues a unique approval code within the tenant. | S |
| FR-VIS-02 | Guards shall verify an approval code and check the visitor in; walk-ins shall be recordable directly (name, vehicle no, reason, host resident). | S |
| FR-VIS-03 | Guards shall record entry and exit times; an on-site list (entered, not exited) shall be visible to Guards/Managers. | S |
| FR-VIS-04 | Approval codes shall be single-use and valid only on the visit date. | S |

### 4.12 Notifications (FR-NOT) — Should (interface from day 1)

| ID | Requirement | Pri |
|---|---|---|
| FR-NOT-01 | The system shall write in-app notification records (user, title, message, read flag) for: new complaint (→ Managers), complaint status change (→ complaint's resident), booking decision (→ booker), new bill (→ unit's residents). | S |
| FR-NOT-02 | Users shall list their notifications and mark them read; unread count available for the UI badge. | S |
| FR-NOT-03 | The notification dispatch shall be behind a service interface so email/SMS/push channels can be added without touching call sites. | M (interface) |

### 4.13 Audit (FR-AUD) — Must (lightweight from day 1)

| ID | Requirement | Pri |
|---|---|---|
| FR-AUD-01 | The system shall append an activity log (user, action, IP, timestamp) for: login success/failure, logout, token refresh, user/role/permission changes, tenant changes, bill/payment mutations, and all deletes. | M |
| FR-AUD-02 | Activity logs shall be append-only via the application (no update/delete endpoints); Admin may read their tenant's logs, Super Admin may read platform logs. | M |
| FR-AUD-03 | Audit writes shall be non-blocking (async) and their failure shall never fail the business operation, but shall be surfaced in application logs. | M |

---

## 5. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-PERF-01 | Performance | 95th-percentile API response < 200 ms for CRUD reads at v1.0 scale (≤ 50 tenants, ≤ 20k units total); list endpoints paginated (default 20, max 100). |
| NFR-PERF-02 | Performance | First contentful paint < 3 s on a mid-range laptop over 10 Mbps; frontend bundle code-split per route. |
| NFR-SEC-01 | Security | Tenant isolation enforced at three independent layers (JWT claim, TenantContext, PostgreSQL RLS with `FORCE ROW LEVEL SECURITY` and a non-owner app DB role). A single-layer failure shall not leak data. |
| NFR-SEC-02 | Security | OWASP Top 10 addressed; all input validated at the boundary (Bean Validation / Zod); no string-concatenated SQL; dependency scanning (OWASP Dependency-Check, npm audit) gate CI at high/critical. |
| NFR-SEC-03 | Security | No secrets in the repository ever; configuration via environment variables; `.env.example` documents every variable. |
| NFR-SEC-04 | Security | Cross-tenant access attempts shall return 404 (resource invisible), never 403 (existence leak). |
| NFR-REL-01 | Reliability | Financial writes transactional; no bill/payment can be half-written. Tenant onboarding atomic (FR-TEN-02). |
| NFR-REL-02 | Reliability | RPO ≤ 24 h (nightly DB backup) and RTO ≤ 4 h at v1.0; both tightened post-v1.0. Restore procedure rehearsed before production go-live. |
| NFR-AVL-01 | Availability | v1.0 target 99.5 % monthly (single-node PaaS acceptable); health endpoint (`/actuator/health`) for platform probes. |
| NFR-MNT-01 | Maintainability | Backend line coverage ≥ 70 % (JaCoCo, CI-enforced on changed files), frontend ≥ 60 % (Vitest); ArchUnit rules enforce package boundaries and `TenantAwareBaseEntity` inheritance. |
| NFR-MNT-02 | Maintainability | API contract is OpenAPI 3, generated from code; frontend types generated from the contract (`openapi-typescript`) — hand-written API types are forbidden. |
| NFR-ACC-01 | Accessibility | WCAG 2.1 AA: 4.5:1 contrast, full keyboard operability, visible focus, labels on all inputs, 44×44 px touch targets. |
| NFR-I18N-01 | i18n | All user-facing strings behind an i18n layer from the first screen (EN shipped; BM addable without code changes). Dates/times stored UTC, displayed Asia/Kuala_Lumpur. |
| NFR-OBS-01 | Observability | Structured JSON logs with `tenant_id`, `user_id`, `request_id` correlation; Spring Actuator + Prometheus metrics; error rate and p95 latency visible per environment. |
| NFR-CMP-01 | Compliance | PDPA: synthetic data only in seeds/tests/fixtures; no personal data in logs; data export/delete-on-request achievable per tenant (documented procedure, not necessarily self-serve in v1.0). |
| NFR-PORT-01 | Portability | Runs anywhere Docker runs; no PaaS-proprietary services in v1.0 (file storage behind a `StorageService` interface: local FS dev → S3-compatible prod). |

---

## 6. Constraints

| ID | Constraint |
|---|---|
| CON-1 | Stack: Spring Boot 4.1.2 / Java 17 source / Maven / native PostgreSQL 16 / Flyway / planned React 18 / TypeScript / Vite / Tailwind. ADR `0002-supported-spring-baseline.md` supersedes the old Boot 3.x version pin after the supported-version review; the architecture and dual-role/isolation requirements remain binding. |
| CON-2 | Development on Windows 11; dev database via Docker Compose only (no host PostgreSQL install). |
| CON-3 | Two part-time developers (~6 h/week each); roadmap must be sliced into evening-sized tasks. |
| CON-4 | Git workflow: user reviews every commit message before commit; no push without explicit instruction; no direct commits to `main`/`dev` once the repo exists. |
| CON-5 | TDD is mandatory for all implementation (root SOP); tenancy/security code additionally requires the isolation test suite green before any feature module starts. |
| CON-6 | Naming is BAiTI everywhere in new artifacts: folder `baiti`, repo `baiti`, package `my.baiti`, class `BaitiApplication`. Legacy files keep their names. |
| CON-7 | Budget ≈ hobby-tier: one small PaaS instance + managed/containerised Postgres must suffice for v1.0. |

---

## 7. Assumptions

| ID | Assumption | If false → |
|---|---|---|
| ASM-1 | The 2026-04-08 stack decision still stands (strongly implied by the 2026-05-19 launch prompt). | Re-run stack decision; blueprint architecture docs must be regenerated. |
| ASM-2 | Anep accepts the MVP module split proposed in the meeting brief. | Re-prioritise §4 M/S labels; roadmap epics re-order (structure unaffected). |
| ASM-3 | v1.0 scale ≤ 50 tenants, ≤ 2 000 units/tenant, ≤ 10 concurrent staff users/tenant. | Revisit NFR-PERF-01 targets and index strategy (04_DATABASE_DESIGN §8). |
| ASM-4 | Payments are recorded manually by staff in v1.0 (no gateway). | Finance module scope grows; add gateway epic. |
| ASM-5 | A single role per user is sufficient (matches PDF `users.role_id`). | Move to user↔roles pivot; schema + RBAC changes (04_DATABASE_DESIGN notes the escape path). |

---

## 8. Unknowns & Open Questions (carried from decision register, 00_INVENTORY §5)

1. **D1** — Did the Anep meeting happen? Who owns the frontend? *(Blocks frontend packets only.)*
2. **D2** — GitHub location/visibility. *(Blocks first push only.)*
3. **D3** — Deployment target. *(Blocks Milestone M6 only; decision matrix ready in 03_ARCHITECTURE §12.)*
4. Bill types: is `billing_month` maintenance-fee-only, or are ad-hoc bills (fines, deposits) needed in v1.0? **Blueprint baseline: single bill type `MAINTENANCE_FEE` plus free-text `description`; `bill_type` enum column reserved.**
5. Should Guards see resident contact details? **Blueprint baseline: no — Guards see names/units only** (least privilege; reversible via permission grant).
6. File uploads (payment proof, announcement attachments): max size/type? **Blueprint baseline: 5 MB; jpg/png/pdf; stored via StorageService; virus scanning out of scope v1.0.**

---

## 9. Acceptance of this SRS

This SRS is implementation-ready as a baseline. Items marked with a "Blueprint baseline/decision" are deliberately decided-but-reversible so that work is never blocked on a stakeholder answer; each is flagged in the roadmap where a cheap reversal window closes.

*Traceability: BR → FR mapping is 1:N and evident from module groupings; FR → test mapping is defined in 06_TESTING_STRATEGY §9; FR → packet mapping in 08_ai_packets/README.*
