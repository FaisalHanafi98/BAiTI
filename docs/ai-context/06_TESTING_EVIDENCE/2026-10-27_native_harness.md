# F0 increment: native PostgreSQL test harness

Started 27 October 2026; updated 29 October 2026. Scope authorised by the user's instruction to continue
implementation, using the accepted native-database/deferred-Docker plan.
Branch: `test/native-postgres-harness`; starting HEAD
`ecb30bf0f4015aa7cff1ec01e9ee730750f6b2df`. Earlier dirty/untracked work and
the local V18 correction are preserved. The owner's later 29 October direction
authorises local commits after the shared SOP, Humanizer and relevant checks;
explicit push authorisation remains separate.

Outcome: replace the health test's unconditional container bootstrap with a
shared explicit native target configuration and preflight before Flyway or the
application touches the database. No RA features, tenancy production code,
schema changes, Docker, hosting, deployment or remote changes in this increment.

Cross-layer path: test environment → native configuration validation → JDBC
target/role/ownership checks → Spring dynamic app/Flyway settings → HealthIT.
Credentials stay out of URLs, source, reports and exception output. No automatic
database creation, clean/reset, role mutation or Docker fallback in the harness.

Acceptance: a complete native configuration is accepted; missing/unsafe settings
fail before connecting; normal dev/default databases and credential-bearing URLs
are rejected; configuration diagnostics redact secrets; only verified native
PG16 targets with distinct intended roles and a matching test marker are used.
Existing health/runtime assertions remain discoverable by Failsafe. Actual DB
checks are separate from configuration unit-test evidence and full G-RLS.

## Native database discovery

Read-only inspection found no psql/pg_ctl/initdb on PATH, no PostgreSQL process,
no installation in the four checked standard locations or the checked PostgreSQL
installation registry key, and no successful listener connection at
127.0.0.1:5432. No local `.env` was found in the project root/backend; only
`.env.example` is present. No TEST_DB/BAITI_TEST process variables were present.
Windows service inventory was unavailable because Service Control Manager access
was denied. These checks do not exhaust custom paths, non-default ports or remote
servers. Repository database-state evidence says V1–V28 is authored, with applied
history unverified; no evidence establishes that BAiTI's database was ever created.

No database was queried, created, migrated or reset during discovery.

## Execution and result

The initial discovery led to an explicitly owned private PostgreSQL 16.15
runtime, native harness/preflight and a working runner. Initial red/green logs
are under `docs/audits/native-harness-2026-10-27/`; ordered clean-export and later
repair evidence are under `docs/audits/foundation-2026-10-29/`. The structured
[verification record](../../audits/foundation-2026-10-29/VERIFICATION.json) records
target identity without credentials, counts, upgrade preservation and hashes.
This is foundation evidence; P05 and complete P03/P04 acceptance are not inferred
from the suite total.

## Separate narrow repair: activity-log address mapping

Full native verify (`18-native-full-verify.log`) reproduced Hibernate schema
validation failure: `activity_logs.ip_address` is INET but ActivityLog's String
mapping expects VARCHAR. Existing SQL and the normative database design agree
on INET. The repair scope is the Java field mapping only, plus a focused test
that uses the actual entity's Hibernate binder against real PG16 for synthetic
documentation-range IPv4/IPv6 values. Keep this as a separate atomic change
from the native harness; no migration edits or claimed full repository round trip.
Reference: [Hibernate 6.5 InetAddress mapping](https://docs.hibernate.org/orm/6.5/userguide/html_single/#basic-inetaddress).

The next full verify (`20-native-full-verify.log`) confirmed the address binding
tests pass and exposed a second scalar mismatch: bills.currency is CHAR(3), but
the Java String mapping expects VARCHAR(3). The same narrow scalar-alignment
repair explicitly uses JDBC CHAR, preserving String/BigDecimal semantics and
the existing SQL. HealthIT's schema validation is its failing regression.

## 29 October: tenant/audit association repair

CoreRepositoryIT reproduced an app-role save/flush failure: Hibernate inserted
only role_id/permission_id and PostgreSQL rejected the row under RLS because
roles_permissions.tenant_id was absent. Narrow repair: map RolePermission with
the existing composite key and tenant/audit fields; extract the shared scoped
mapped superclass from the UUID-id subclass. No migration or new id column.
Use explicit grant/revoke operations and a read-only permission projection;
this is an internal entity API refinement before business endpoints exist.
Add same-tenant grant, duplicate/revoke and foreign-tenant negative coverage.
Raw same-tenant FK enforcement and production onboarding remain separate work.
The privileged A/B tenant seed is a named synthetic fixture, not application
onboarding proof. HealthIT uses a test-only fixed tenant until P05 exists.

## Next bounded F0/F1 increment — history and platform access

Owner authorisation: continue implementation and local commits after the SOP,
Humanizer and relevant verification. Native foundation, permission persistence
and V29 are now separately committed within one planned foundation PR.

Characterise the remaining named history/platform risks using synthetic fixtures
only. Test app and ordinary owner inserts into global tables under the actual
forced policies; record the missing platform provisioning path explicitly.
For complaints, verify that parent deletion with history respects the existing
RESTRICT FK and that removing a collection entry cannot silently erase its row.
Retain a positive test for cascading insertion of a new history entry.

If deletion regressions fail through ORM cascade/orphan removal, restrict only
that association's cascading delete behaviour. Preserve the SQL/applied history,
other associations and insertion path. Tests roll back their own synthetic data;
no original complaint is read or changed. This is not a full append-only policy
or production authorisation implementation. P05 and global provisioning still
need their explicit privilege design and complete local G-RLS evidence.

Observed red: `36-history-preservation-red.log` ran 57 unit tests and 19 focused
integration cases; exactly two history-protection cases failed without errors
or skips. Parent removal deleted its child history and clearing the collection
deleted the history row. The positive history insertion case passed. Four global
provisioning probes confirmed app and ordinary migrator inserts are denied with
SQLSTATE 42501; FORCE RLS binds the owner, so the platform path remains unresolved.

Selected repair: restrict Complaint.updates cascading to PERSIST/MERGE and remove
orphan removal. No DDL, grant or applied migration change. The same parent delete
must then fail with SQLSTATE 23503 while the insertion positive control still
succeeds. Full-suite green evidence follows only after actual verification.

Green: `38-history-preservation-green-full.log` passed **57 unit/architecture +
45 integration tests**, zero failures/errors/skips, in 45.730 seconds. The parent
delete now reaches PostgreSQL's FK and fails with 23503, collection clearing
retains the history row, and cascading insertion still succeeds. Four platform
characterization cases pass by observing the current 42501 denials; they record
the missing provisioning path, not a working platform implementation. Attempt 37
stopped at Spotless before tests; the required annotation formatting was applied
before the successful rerun. No SQL or applied history changed for this repair.

## P03/P04 acceptance mapping at this checkpoint

| Requirement | Evidence / remaining work |
|---|---|
| P02 boot/health and actual app principal | HealthIT passes with real native PG16, non-owner app role and labelled test-only tenant resolver |
| P03 fresh migrations and policy/grant assertions | Clean export applied V1–V28; V29 has earlier fresh-install evidence and a new V28→V29 upgrade preserving named tenants plus a separate sentinel |
| Missing Flyway resources fail visibly | MigrationLocationsIT reproduced the old relaxation and passes after removing it |
| Legacy SQL typo check | Checked migration source separately; result is recorded in the session evidence |
| P04 mapping/aggregate round trips | Hibernate validate and all 24 tenant-entity mappings covered collectively; global mapping validation does not establish global provisioning |
| Uniqueness/enum/time/actor cases | Selected case-insensitive email, duplicate unit, enum/time/money/actor/INET/JSON cases pass; exhaustive constraint and enum-transition coverage remains open |
| Architecture and fetch rules | Five architecture rules pass; controller/JWT conditions are currently vacuous. Relationship fetch configuration is inspected separately |
| Historical records | Payment/audit mutation denied; complaint cascade/orphan deletion repaired. Raw complaint-history immutability and production authorisation are not established |
| Platform provisioning | Confirmed ordinary app/migrator writes are denied under current forced policies; explicit platform policy and real onboarding still required |
| Isolation and release | Raw database smoke is verified. Production context/pooling, all I1–I9, changed-line coverage, GitHub CI and release remain open |

Local source commits: `3c78d92` (V18 correction), `cb2399d` (native harness),
`e2bb68c` (permission persistence) and `aa126eb` (V29). They are intended to form
one meaningful foundation PR with this follow-up repair. No push or PR occurred.
