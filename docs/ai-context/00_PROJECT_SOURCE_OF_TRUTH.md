# MyComunity — Project Source of Truth

> **Last Verified**: 2026-05-10
> **Evidence Basis**: Direct file inspection of `Anep's Project/` folder
> **Confidence**: HIGH for planning facts; UNVERIFIED for execution state (no code exists yet)

---

## Stable Project Facts

### Purpose
Multi-tenant SaaS condominium/community management system. One deployment serves N condos (tenants). Each condo's data is completely isolated from others.

### Collaborators
- **Faisal** (faisalhanafi.dsa@gmail.com) — backend lead, architecture, schema, auth
- **Anep** — frontend lead (React UI). Stack alignment meeting was planned for 2026-04-08.

### Confirmed Tech Stack (VERIFIED — documented in `MyComunity_PLAN.txt` 2026-04-08)

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.x, Java 17 (Amazon Corretto), Maven |
| Database | PostgreSQL 16 with Row-Level Security (RLS) |
| ORM | Spring Data JPA / Hibernate |
| Auth | Spring Security + JWT (HS256), bcrypt cost 12 |
| Frontend | React 18 + TypeScript + Vite |
| Styling | Tailwind CSS + Headless UI |
| State / Data | TanStack Query + Zustand |
| Forms | React Hook Form + Zod |
| HTTP client | Axios with JWT refresh interceptor |
| API spec | OpenAPI 3 (auto-generated types via `openapi-typescript`) |
| Routing | React Router v6 |
| Testing backend | JUnit 5 + Mockito + Testcontainers (PostgreSQL) |
| Testing frontend | Vitest + React Testing Library |
| E2E | Playwright (post-MVP) |
| Migrations | Flyway |
| CI/CD | GitHub Actions |
| Local infra | Docker Compose (postgres:16 + pgadmin) |

### Repository Layout (PLANNED — not yet created)

```
MyComunity/
├── backend/                    # Spring Boot 3.x (Maven)
│   ├── src/main/java/my/comunity/...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/       # Flyway V1__*.sql ...
│   └── pom.xml
├── frontend/                   # Vite + React 18 + TypeScript
│   ├── src/
│   ├── tests/
│   ├── package.json
│   └── vite.config.ts
├── infra/
│   ├── docker-compose.yml      # postgres + pgadmin (dev)
│   ├── Dockerfile.backend
│   └── Dockerfile.frontend
├── docs/
│   ├── ai-context/             # This archive
│   ├── specs/
│   ├── plans/
│   └── adr/
├── .github/workflows/
├── .gitignore
├── .env.example
├── README.md
└── CLAUDE.md
```

### Naming Conventions

- Java package root: `my.comunity` (single-m, matching project name)
- Table names: snake_case, plural
- Java classes: PascalCase
- API endpoints: REST, kebab-case paths (e.g. `/api/complaints`, `/api/facility-bookings`)
- Flyway files: `V{n}__{description}.sql` (incrementing, never edit after run)
- Commits: `type(scope): description` (Conventional Commits)

### User Roles (from data model + plan)

| Role | Description |
|---|---|
| Super Admin | Manages tenants; cross-tenant access |
| Admin | Tenant-level admin; manages users, roles, settings |
| Manager | Operations; approves complaints, manages facilities |
| Resident | Views own unit/bills; submits complaints |
| Guard | Manages visitors at entry points |

### Architecture Principles (NON-NEGOTIABLE)

1. **Multi-tenant from day 1** — every domain table has `tenant_id UUID NOT NULL REFERENCES tenants(id)`
2. **Three-layer tenant isolation** — JWT claim → TenantContext (ThreadLocal) → PostgreSQL RLS
3. **RLS is mandatory** — `SET LOCAL app.current_tenant = '<uuid>'` per request, enforced at DB layer
4. **Package-by-feature** — not package-by-layer
5. **Every domain entity extends `TenantAwareBaseEntity`** — enforced by ArchUnit test
6. **TDD mandatory** — failing test first, then implementation
7. **No secrets in commits** — `.env.example` only, pre-commit hook required
8. **No direct pushes to `main` or `dev`** — PRs only
9. **Commit workflow** — Faisal drafts message → user reviews/edits → Faisal commits. Never auto-push.

### Data Model — 23 Tables (22 from PDF + tenants added)

| Domain | Tables |
|---|---|
| Tenancy (added) | `tenants` |
| Property | `blocks`, `units` |
| People / RBAC | `users`, `roles`, `permissions`, `roles_permissions`, `residents`, `family_members` |
| Finance | `bills` (normalized from PDF's `billings`), `payments`, `payment_methods` |
| Complaints | `complaints`, `complaint_categories`, `complaint_updates` (normalized from PDF's `complaints_updates`) |
| Maintenance | `maintenance_requests` |
| Facilities | `facilities`, `facility_bookings` |
| Community | `announcements`, `events` |
| Visitors | `visitors`, `visitor_pre_registrations` |
| System | `notifications`, `activity_logs`, `refresh_tokens` |

**Typo corrections from PDF (do NOT replicate):**
- `residents.resident_type` → enum `OWNER | TENANT`
- `announcements.tittle` → `title`
- `events.tittle` → `title`
- `events.loacation` → `location`
- `notifications.messaage` → `message`
- `billings` → `bills`
- `complaints_updates` → `complaint_updates`

### Planned Flyway Migration Sequence (PLANNED)

V1 → extensions (uuid-ossp, pgcrypto)
V2 → tenants
V3 → blocks, V4 → units
V5–V8 → permissions, roles, roles_permissions, users
V9 → residents, V10 → family_members
V11–V13 → payment_methods, bills, payments
V14–V16 → complaint_categories, complaints, complaint_updates
V17 → maintenance_requests
V18 → facilities, V19 → facility_bookings
V20 → announcements, V21 → events
V22 → visitors, V23 → visitor_pre_registrations
V24 → notifications, V25 → activity_logs
V26 → enable_rls (RLS policy on every domain table)
V27 → seed_roles_permissions (template per tenant)
V28 → refresh_tokens
V29 → seed_dev_data (TWO synthetic tenants)

### JWT + Auth Design (PLANNED)

- Access token: 15 min, HS256, claims = `userId`, `tenantId`, `roles`, `permissions`
- Refresh token: 7 days, rotating, stored server-side in `refresh_tokens` table
- Endpoints: `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`
- Tenant resolution dev: `X-Tenant-Slug` header
- Tenant resolution prod: subdomain (`condoA.mycomunity.app`)

### Test Coverage Targets (PLANNED)

- Backend: ≥70% line coverage (JaCoCo enforced in CI)
- Frontend: ≥60% line coverage (Vitest)
- Integration: All repositories + controllers (Testcontainers)
- Cross-tenant isolation: Mandatory test proving tenant A cannot read tenant B

### Links to Planning Documents

| Document | Location | Status |
|---|---|---|
| Full SDLC Plan | `MyComunity_PLAN.txt` | VERIFIED — current, comprehensive |
| Meeting Brief (Anep) | `meeting/01_MEETING_BRIEF.md` | VERIFIED — prep doc for 2026-04-08 meeting |
| Stack Rationale | `meeting/02_STACK_RATIONALE.md` | VERIFIED — current |
| Spring Boot Refresher | `meeting/03_SPRINGBOOT_REFRESHER.md` | VERIFIED — reference doc |
| React Refresher | `meeting/04_REACT_REFRESHER.md` | VERIFIED — reference doc |
| Original Data Model | `MyComunity 1.0 data model.pdf` | VERIFIED — source schema (22 tables) |
| Old Session Init (pre-stack-decision) | `.orchestrator/sessions/MYCOMUNITY_SESSION_2026-04-01.md` | STALE DOC — stack options listed, not yet decided |

---

## What Is NOT Stable (Do Not Treat as Fixed)

- **Directory name**: still `Anep's Project` — pending rename to `MyComunity`
- **GitHub repo location**: unresolved (personal account vs shared org)
- **Anep alignment**: meeting was planned but outcome UNVERIFIED
- **Deployment target**: deferred to Phase 11
- **Mobile timeline**: deferred
- **Language support**: English only confirmed; BM not decided
- **Billing currency**: MYR assumed, multi-currency not decided
- **Super-admin model**: recommended `super_admins` table but not finalized
