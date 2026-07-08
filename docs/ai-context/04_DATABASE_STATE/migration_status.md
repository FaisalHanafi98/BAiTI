# Database Migration Status

> **Last Updated**: 2026-05-10
> **Evidence Basis**: Planning docs only — no database exists yet

---

## Current State

**No database. No migrations. No PostgreSQL installed.**

The migration sequence is fully designed in `MyComunity_PLAN.txt` but nothing has been executed.

---

## Planned Migrations (PLANNED — not created yet)

| Version | Description | Status | Risk Notes |
|---|---|---|---|
| V1 | Extensions (uuid-ossp, pgcrypto) | PLANNED | Must run first — uuid generation depends on this |
| V2 | `tenants` table | PLANNED | Root entity — all domain tables reference this |
| V3 | `blocks` | PLANNED | Requires `tenants` |
| V4 | `units` | PLANNED | Requires `blocks`, `tenants` |
| V5 | `permissions` | PLANNED | Requires `tenants` |
| V6 | `roles` | PLANNED | Requires `tenants` |
| V7 | `roles_permissions` | PLANNED | Pivot; requires `roles`, `permissions` |
| V8 | `users` | PLANNED | Requires `roles`, `tenants`; UNIQUE(`tenant_id`, `email`) |
| V9 | `residents` | PLANNED | Requires `users`, `units`, `tenants` |
| V10 | `family_members` | PLANNED | Requires `residents`, `tenants` |
| V11 | `payment_methods` | PLANNED | Requires `tenants` |
| V12 | `bills` | PLANNED | Normalized from `billings`; requires `residents`, `tenants` |
| V13 | `payments` | PLANNED | Requires `bills`, `payment_methods`, `tenants` |
| V14 | `complaint_categories` | PLANNED | Requires `tenants` |
| V15 | `complaints` | PLANNED | Requires `residents`, `complaint_categories`, `tenants` |
| V16 | `complaint_updates` | PLANNED | Normalized from `complaints_updates`; requires `complaints`, `tenants` |
| V17 | `maintenance_requests` | PLANNED | Requires `tenants`, likely `units` |
| V18 | `facilities` | PLANNED | Requires `tenants` |
| V19 | `facility_bookings` | PLANNED | Requires `facilities`, `residents`, `tenants` |
| V20 | `announcements` | PLANNED | Requires `tenants` |
| V21 | `events` | PLANNED | Requires `tenants` |
| V22 | `visitors` | PLANNED | Requires `tenants` |
| V23 | `visitor_pre_registrations` | PLANNED | Requires `visitors`, `residents`, `tenants` |
| V24 | `notifications` | PLANNED | Requires `users`, `tenants` |
| V25 | `activity_logs` | PLANNED | Requires `users`, `tenants` |
| V26 | enable_rls | PLANNED | **CRITICAL** — RLS policy on every domain table. No domain queries safe before this runs. |
| V27 | seed_roles_permissions | PLANNED | Template roles/perms to copy per tenant on creation |
| V28 | `refresh_tokens` | PLANNED | Requires `users`, `tenants` |
| V29 | seed_dev_data | PLANNED | Two synthetic tenants (dev profile only); cross-tenant isolation test data |

---

## Pending Migrations

All 29 are pending. None have been run.

---

## Known Migration Risks

| Risk | Detail | Mitigation |
|---|---|---|
| V26 (RLS enable) must run after all domain tables | If run before tables exist, policies fail | Strict version ordering; Flyway enforces this |
| Never edit existing migration files | Flyway checksum validation will fail | Always create `V{n+1}__` for fixes |
| V29 (seed_dev_data) must be dev-profile only | Synthetic data must not reach production | Flyway `locations` config splits dev/prod seeds |
| PDF has typos — do not replicate | See plan for correction list | Source of truth is the plan, not the PDF |
| `tenant_id` missing on any table | Cross-tenant isolation broken | Review checklist: every table after V2 must have `tenant_id UUID NOT NULL REFERENCES tenants(id)` |

---

## How to Check Migration State Once Database Exists

```bash
# Via Spring Boot Actuator (after setup)
curl http://localhost:8080/actuator/flyway

# Via psql
psql -h localhost -U postgres -d mycomunity -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```
