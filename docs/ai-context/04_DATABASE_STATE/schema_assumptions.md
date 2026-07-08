# Database Schema Assumptions

> **Last Updated**: 2026-05-10
> **Evidence Basis**: `MyComunity_PLAN.txt` + `MyComunity 1.0 data model.pdf`
> **Confidence**: INFERRED from planning documents — not yet verified against actual database

---

## What the Plan Expects (From Source Documents)

### Universal Column Pattern (Every Domain Table)

Every table after `tenants` must have:
```sql
tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT
```

And for `TenantAwareBaseEntity` (the base Java entity all domain entities extend):
```sql
id          UUID PRIMARY KEY DEFAULT uuid_generate_v4()
tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT
created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
created_by  UUID REFERENCES users(id)
updated_by  UUID REFERENCES users(id)
```

### RLS Policy Pattern (V26 — to be applied to every domain table)

```sql
ALTER TABLE {table_name} ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON {table_name}
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
```

### Composite Unique Constraints

- `users`: `UNIQUE (tenant_id, email)` — email must be unique within a tenant
- Other unique constraints TBD per domain table during migration design

### Key Enum Types

- `residents.resident_type`: `OWNER` | `TENANT`
- Additional enums TBD per domain (complaint status, payment status, etc.)

---

## Mismatches Between PDF and Plan

| Field | PDF (Original) | Plan (Corrected) | Reason |
|---|---|---|---|
| `announcements.tittle` | `tittle` | `title` | Typo in PDF |
| `events.tittle` | `tittle` | `title` | Typo in PDF |
| `events.loacation` | `loacation` | `location` | Typo in PDF |
| `notifications.messaage` | `messaage` | `message` | Typo in PDF |
| Finance table name | `billings` | `bills` | Normalized |
| Complaint updates table | `complaints_updates` | `complaint_updates` | Normalized |
| `residents.resident_type` values | "Owner/Type" | `OWNER` \| `TENANT` | Typo corrected |

**Source of truth for schema: the plan (`MyComunity_PLAN.txt`), not the PDF.**

---

## Required Validation Before Schema Changes

Before any Flyway migration is written or run:

- [ ] Does this table have `tenant_id UUID NOT NULL REFERENCES tenants(id)`?
- [ ] Does V26 (enable_rls) include a policy for this table?
- [ ] Is there a composite UNIQUE constraint that includes `tenant_id` where needed?
- [ ] Are all FK targets already created (earlier migration)?
- [ ] Are typos from the PDF corrected?
- [ ] Is the migration number strictly higher than all existing migrations?
- [ ] Is the migration file name format correct: `V{n}__{description}.sql`?

---

## Current Database State

**No database exists.** PostgreSQL not installed. No tables created. No data.

Verify by running (once Docker is running):
```bash
docker compose up -d
psql -h localhost -p 5432 -U postgres -c "\l"
```
