# Tenant isolation verification

Run the complete suite on an owned PostgreSQL 16 test target using the
[native guide](../testing/NATIVE_POSTGRES.md). To repeat the gate's named cases
and the schema checks, run this from the project root with private test settings.

```powershell
./tools/testing/Invoke-NativeTests.ps1 -IntegrationTests TenantIsolationIT,PlatformAuthIT,MigrationIT
```

| Cases | What the run checks |
|---|---|
| I1–I2 | The signed caller sees its own units through real Hibernate repositories. An existing foreign unit returns the same neutral 404 as an unknown unit. |
| I3 | An authenticated write referencing a foreign block returns 404. A valid same-tenant write succeeds. The app pool also rejects the foreign block reference in SQL with 23503 and leaves no matching row. |
| I4 | A signed token presented for a different active slug fails with 401 before the request reaches the test route. |
| I5–I7 | Raw app-pool SQL without scope sees no units and cannot insert. With scope the pool runs as `baiti_app`, lacks bypass rights, and sees the local unit but not the foreign unit. |
| I8 | The real async worker writes both tenants' sign-ins to their own audit logs. It also records a failed sign-in without an actor and clears the worker's context. |
| I9 | The platform onboarding transaction rolls back tenant, roles, permissions, grants, admin and audit rows after an injected failure at step four. |

`TenantIsolationIT` onboards two synthetic communities on the first run. The
verified database ownership marker determines their reserved slugs. Later runs
check the original onboarding audit and administrator identity before reusing
them. The baseline communities stay in that test database because the normal
platform role has no tenant-delete policy. Each run removes its own units,
blocks, sign-in users and auth audit rows by exact IDs or test address. The
unit routes exist only in test sources.

When adding a tenant table, require FORCE RLS with both read and write
predicates. Pair tenant-owned foreign keys with `tenant_id` and add the
matching indexes. Add same-tenant success and foreign-ID denial at each new
HTTP entry point. A write service resolves referenced rows with the caller's
scoped repository before it stores the relationship. The database constraint
provides a second check for SQL paths that skip the repository lookup. Add
same-tenant ownership tests when a route is limited to the resident's records.

The [testing strategy](../blueprint/06_TESTING_STRATEGY.md) defines the release
gate. Run the full suite and permission matrix before opening feature work.
GitHub checks and deployment require their own results.
