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

## Verified foundation checkpoint — 29 October 2026

The complete runner passed **57 unit/architecture + 45 integration tests**, with
no failures, errors or skips. See the committed
[verification record](../audits/foundation-2026-10-29/VERIFICATION.json) and
[acceptance mapping](../ai-context/06_TESTING_EVIDENCE/2026-10-27_native_harness.md).
The ordered clean-export scopes passed 56, 81 and 95 tests; the later history
repair and platform characterization increased the complete suite to 102.

Fresh V1–V28 and V28→V29 upgrade evidence are distinct. The upgrade retained both
named synthetic tenants and a sentinel in its own schema; no existing target was
reset. V29 also has earlier fresh-install evidence. Complaint history survives
parent/collection deletion attempts through the tested mapping, while new
history insertion still works. Current global policies deny app and ordinary
migrator provisioning; the separate platform policy is still required.

The next runtime work is [ADR 0002](../adr/0002-supported-spring-baseline.md).
Do not infer full P03/P04 acceptance, production tenant resolution, exhaustive
constraint/enum coverage, I1–I9, changed-line coverage or hosted success from the
foundation suite. The current private cluster is retained for synthetic tests.

## Supported-runtime checkpoint — 29 October 2026

On `chore/supported-spring-baseline`, Boot 4.1.2/springdoc 3.2.0 pass **105 tests**:
58 unit/architecture and 47 integration cases, without failures/errors/skips.
The extra cases verify the loaded Boot line and authenticated OpenAPI generation
with anonymous denial. Existing role, schema, boundary and history checks remain
in place. A separate empty native database applied all 29 unchanged migrations.
See [the runtime audit](../audits/runtime-upgrade-2026-10-29/VERIFICATION.json).

Private settings for this isolated worktree are supplied with `-SettingsPath`
pointing to the original project's ignored runtime folder. The test runner still
loads only the seven test keys and never provisions or resets a database.
Production-profile startup and real JWT/context/onboarding depend on P05; the
test fixture and authenticated mock user are not production authentication proof.

## Platform-policy checkpoint — 29 October 2026

V30 supplies the explicit trusted-owner global policies from
[ADR 0003](../adr/0003-explicit-platform-table-policies.md). The suite passes
111 tests on upgraded and fresh targets: 58 unit/architecture and 53 integration,
without failures/errors/skips. All V1–V29 source hashes are preserved.

After verified migration of a new owned test database, explicitly apply both
`fixtures/foundation-tenants.sql` and `fixtures/foundation-platform.sql` from
`backend/src/test/resources/` through the intended `baiti_migrator` connection.
These are named synthetic fixtures; the platform hash is a non-login placeholder.
ON CONFLICT does not overwrite existing rows. Tests verify the canary identity,
and all ordinary test mutations roll back. The runner itself still does not
provision, clean or reset fixtures. Keep credentials in the private settings.

The platform canary proves the app's global read/update/delete denials against
an existing row. SET ROLE escalation and owner domain-scope/reset checks are
also exercised. The owner has DML SELECT/INSERT/UPDATE on the two global tables,
with no DELETE policy; its trusted DDL ownership is unchanged. These checks do
not implement the separate P05 HTTP principal, privileged pool or onboarding.
See [the policy audit](../audits/persistence-acceptance-2026-10-29/VERIFICATION.json).

## Persistence acceptance checkpoint — 29 October 2026

The final P03/P04 local suite passes 159 tests (59 unit/architecture and 100
integration), without failures/errors/skips, on upgraded V30→V31 and fresh
V1–V31 targets. Fresh migrations were tested before explicit fixture provisioning.
See [the acceptance mapping](../ai-context/06_TESTING_EVIDENCE/2026-10-29_persistence_acceptance.md)
and [structured evidence](../audits/persistence-acceptance-2026-10-29/P04_ACCEPTANCE.json).
The older checkpoints above retain their original scope and counts. P05's
production tenant context, authentication and onboarding remain unfinished.
