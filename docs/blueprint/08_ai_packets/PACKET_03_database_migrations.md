# P03 — Database Migrations V1–V28

Current execution amendment — 29 October 2026: V1–V28 remain the immutable
baseline. Approved forward repairs V29/V30 and their effective policy contracts
are in database blueprint section 5. Native `testing/MigrationIT` now verifies
30 history entries, 26 relations, forced domain isolation and the six bounded
global owner policies. Explicit named synthetic fixtures are test setup, not
production seed migrations or onboarding proof.

**Objective**: The complete BAiTI schema — 26 relations, constraints, indexes, RLS — applied by Flyway and proven by a migration test. The single DDL source is `04_DATABASE_DESIGN.md`; this packet is a transcription task, not a design task.

**Scope**
- In: migration files V1–V28, migration integration test, grants/append-only revokes, removal of P02's `fail-on-missing-locations` relaxation.
- Out: entities (P04), seed data (P05's `DevDataInitializer`), any SQL seeds (§ none exist by design — 04 §6).

**Dependencies**: P02.

**Files to create**
```
backend/src/main/resources/db/migration/
  V1__baseline_extensions_and_grants.sql        V15__payments.sql
  V2__tenants.sql                               V16__complaint_categories.sql
  V3__super_admins.sql                          V17__complaints.sql
  V4__permissions.sql                           V18__complaint_updates.sql
  V5__roles.sql                                 V19__maintenance_requests.sql
  V6__roles_permissions.sql                     V20__facilities.sql
  V7__users.sql                                 V21__facility_bookings.sql
  V8__refresh_tokens.sql                        V22__announcements.sql
  V9__blocks.sql                                V23__events.sql
  V10__units.sql                                V24__visitor_pre_registrations.sql
  V11__residents.sql                            V25__visitors.sql
  V12__family_members.sql                       V26__notifications.sql
  V13__payment_methods.sql                      V27__activity_logs.sql
  V14__bills.sql                                V28__enable_rls.sql
backend/src/test/java/my/baiti/db/MigrationIT.java
```

**Expected architecture (binding rules)**
- Column definitions, checks, uniques, partial indexes: **exactly** 04 §4 per table; base columns per 04 §1; enum CHECKs per 04 §1.6.
- V1: `CREATE EXTENSION IF NOT EXISTS citext;` + `ALTER DEFAULT PRIVILEGES FOR ROLE baiti_migrator IN SCHEMA public GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO baiti_app;` (+ sequences equivalent). No role creation, no passwords (roles exist from P01 bootstrap).
- V28: for each of the 24 tenant tables — `ENABLE`/`FORCE ROW LEVEL SECURITY` + `tenant_isolation` policy with `USING` **and** `WITH CHECK` on `current_setting('app.current_tenant', true)::uuid` (04 §5). Then: `REVOKE UPDATE, DELETE ON payments, activity_logs FROM baiti_app;` `tenants`: self-row SELECT policy; `super_admins`: RLS enabled, zero policies for `baiti_app`.
- Every FK gets an explicit index unless it's the leading column of an existing unique (04 §1.10).
- One table per file; header comment links the 04 §4 subsection; PDF-typo spellings anywhere = defect (04 §9).

**Testing requirements** — `MigrationIT` (06_TESTING §3.1), Testcontainers PG16 **with the two roles created in test setup** (mirror P01's bootstrap SQL in the container init):
1. Flyway (as `baiti_migrator`) applies V1–V28 cleanly on an empty DB.
2. Assert 26 relations exist; spot-check 5 representative column definitions (types + NOT NULL) against 04 §4.
3. Assert from `pg_tables`/`pg_policies`: `rowsecurity=true` AND `forcerowsecurity=true` and a `tenant_isolation` policy for all 24 tenant tables.
4. Assert `baiti_app` has no UPDATE/DELETE on `payments`/`activity_logs` (`has_table_privilege`).
5. RLS smoke (pre-P05, raw JDBC as `baiti_app`): insert tenant A+B as migrator; `set_config('app.current_tenant', A)` sees only A's rows in `blocks`; unset context sees zero; insert into B while set to A is rejected.

**Definition of Done**
- [ ] `./mvnw verify` green including `MigrationIT`; output captured to `docs/audits/` evidence file.
- [ ] `flyway.fail-on-missing-locations` relaxation removed.
- [ ] Grep proves zero occurrences of `tittle|loacation|messaage|premission|bilings|billings` in `db/migration/`.
- [ ] Commits grouped per domain (tenancy/RBAC, property+residency, finance, complaints+maintenance, v1.x tables, system+RLS), each user-approved.
