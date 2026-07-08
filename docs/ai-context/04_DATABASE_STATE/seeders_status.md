# Seeders / Seed Data Status

> **Last Updated**: 2026-05-10
> **Evidence Basis**: Planning docs only — no database exists yet

---

## Current State

No seed data exists. No seeder scripts have been written. No database exists.

---

## Planned Seed Strategy (From `MyComunity_PLAN.txt`)

Seeding is handled via Flyway migration files, not separate seeders. This is intentional — it keeps seed data in the same migration history and ensures dev data is version-controlled.

| Flyway File | Type | Content | Profile |
|---|---|---|---|
| `V27__seed_roles_permissions.sql` | Template seed | Default roles (Admin, Manager, Resident, Guard) + permission sets. Copied per tenant on creation. | All profiles |
| `V29__seed_dev_data.sql` | Dev seed | **Two synthetic tenants** — required for cross-tenant isolation testing. Synthetic users, units, residents. No real personal data. | Dev only |

---

## Required Seed Data (for Phase 0 dev environment to function)

Once PostgreSQL is running and migrations are applied, the following must exist for basic testing:

| Data | Table | Required By | How Created |
|---|---|---|---|
| At least 2 tenants | `tenants` | Cross-tenant isolation test | V29 seed |
| Default roles | `roles` | Auth/RBAC flow | V27 seed |
| Default permissions | `permissions` | Auth/RBAC flow | V27 seed |
| Role-permission mappings | `roles_permissions` | Auth/RBAC flow | V27 seed |
| At least 1 Admin user per tenant | `users` | Login flow | V29 seed |

---

## Known Seeder Order Issues

- V27 (seed_roles_permissions) must run AFTER V5–V7 (permissions, roles, roles_permissions tables)
- V29 (seed_dev_data) must run AFTER V27 (needs roles/permissions to exist)
- V29 must NOT run in production profile — use Flyway `locations` config to exclude
- Two tenants in V29 are required — not one — to enable cross-tenant isolation tests

---

## PDPA / Data Safety Rules

- **No real personal data in any seed file** — PDPA violation if real resident/user data is committed
- Seed data must use synthetic names, emails, and identifiers
- Suggested synthetic tenant names: `Sunway Residence` + `Damansara Heights` (or similar fictional but plausible)
- All seed emails must use `@example.com` or similar non-real domains

---

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| V29 runs in production | Real users get synthetic data mixed in | Strict Flyway profile separation |
| Only one synthetic tenant in V29 | Cross-tenant isolation test cannot run | Always seed TWO tenants |
| Real email/name in seed | PDPA violation, data exposure | Mandatory code review check |
