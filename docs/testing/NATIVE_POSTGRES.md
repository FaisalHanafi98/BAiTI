# Native PostgreSQL foundation tests

Current: 29 October 2026. Docker, hosting/VPS and deployment remain deferred.
The [normative test contract](../blueprint/06_TESTING_STRATEGY.md#36-native-postgresql-execution-contract--accepted-27-october-2026)
owns assertions. This page explains the implemented local harness and runner.

## Test target

A private PostgreSQL 16.15 runtime was prepared under `backend/.local/postgres16/`
from the [Windows binary archive route](https://www.postgresql.org/download/windows/)
linked to [EDB](https://www.enterprisedb.com/download-postgresql-binaries).
Only bin/lib/share were extracted. No Windows service, global PATH change,
Docker or hosted endpoint was created. The local cluster is bound to
`127.0.0.1:55432`; `baiti_test_f0` contains synthetic test fixtures only. It is
separate from any future normal development or production database.

The archive's locally calculated SHA-256 is
`43bb45f173a6f08cf1d29a97a6d8deb119e8e8093a24c00d2d1001a0ccaa8281`.
This records the downloaded bytes; it is not an independently published vendor
checksum. The selected postgres executable reported 16.15 and was unsigned.
Runtime binaries, data, credentials and raw runtime logs are ignored by Git.

An alternative native server must satisfy the same preflight, with separate
`baiti_migrator` and non-owner `baiti_app` roles and an explicitly owned
`baiti_test_*` database. The public schema/database owner must be the migrator.
Preflight rejects superuser/bypass/role-management privileges and extra role
membership; PostgreSQL's implicit `pg_database_owner` membership is handled
separately through the checked database owner. Never alter shared roles casually.

## Settings and ownership

Set these process variables, or provide them in an **ignored local JSON file**
for the runner below. Keep password values out of source and transcripts:

| Setting | Value/meaning |
|---|---|
| `BAITI_TEST_DB_MODE` | `native`; no automatic Docker fallback |
| `TEST_DB_URL` | Explicit JDBC host, port and dedicated `baiti_test_*` DB, without userinfo, query options or fragment |
| `TEST_DB_APP_USER` | `baiti_app` |
| `TEST_DB_APP_PASSWORD` | Private runtime-role credential |
| `TEST_DB_MIGRATOR_USER` | `baiti_migrator` |
| `TEST_DB_MIGRATOR_PASSWORD` | Separate private migrator credential |
| `TEST_DB_OWNER_TOKEN` | Canonical UUID matching the database comment `baiti-test:<UUID>` |

The database owner establishes the marker during explicit provisioning.
Preflight reads it using `shobj_description` and checks both connections before
Flyway. A naming prefix alone does not establish disposal ownership. The
harness never creates/drops/cleans a database, changes roles or resets fixtures.
HealthIT and MigrationIT share this harness; missing targets fail visibly.

The local settings file is `backend/.local/postgres16/test-settings.json` with
access restricted to the current Windows user. It also holds local fixture
administrator settings; the test runner deliberately loads only the seven test
keys above and does not expose administrator credentials to application tests.

## Start and verify

If the private cluster is stopped, run its `pg_ctl.exe` from
`backend/.local/postgres16/dist/pgsql/bin/` with `start`, `-D` pointing to the
exact private `data/` folder, `-l` to its local postgres log, and
`-o "-h 127.0.0.1 -p 55432"`. Use a hidden background process and wait only for
pg_ctl itself to exit; waiting for its entire descendant tree also waits for
the database server. Do not initialise an existing data directory again.

From the project root:

```powershell
./tools/testing/Invoke-NativeTests.ps1
./tools/testing/Invoke-NativeTests.ps1 -IntegrationTests HealthIT
```

The runner defaults to offline Maven, preserves its exit code, restores the
previous process environment and prints no settings. `-Online` allows normal
pinned build dependency resolution when needed; it does not enable the remote
dependency-audit profile. With process variables already set, run `mvn verify`
directly from backend instead. No `skipTests`/`skipITs` workaround closes a gate.

## Foundation fixture boundary

P05 production tenancy is not yet available. Foundation tests explicitly import
`FoundationTenantConfiguration`, which supplies a fixed synthetic tenant to
Hibernate. Repository tests set the matching PostgreSQL GUC on the same test
transaction. This is test scaffolding, **not production tenant-resolution proof**.

`backend/src/test/resources/fixtures/foundation-tenants.sql` supplies only the
two fixed A/B tenants. It was applied by the private cluster's fixture admin;
that privileged setup does not prove the ordinary migrator can onboard tenants.
The repository tests verify fixture identity and roll back their own writes;
they do not delete unrelated data. Provision those named synthetic fixtures
explicitly before running CoreRepositoryIT on another owned test target.

The harness covers configuration/preflight, actual schema/RLS/grants, test
application health/runtime role and IP binding. Dependent foundation changes
extend it with aggregate, permission-link and tenant-setting regressions. Full
G-RLS, authentication, business services, frontend, CI and deployment are
separate unfinished gates. A green foundation run does not close those gates.
