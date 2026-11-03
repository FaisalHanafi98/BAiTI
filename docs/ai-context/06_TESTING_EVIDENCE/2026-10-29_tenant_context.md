# P05 slice 5a — tenant context, 29 October 2026

Based on persistence acceptance c4ffbac, branch feat/tenant-context. Authorised
native runner: explicit private settings, PostgreSQL 16.15, owned synthetic V31
database. No schema/role mutations. [Structured evidence](../../audits/tenant-context-2026-10-29/VERIFICATION.json)
contains log hashes and counts; raw logs remain ignored.

| Boundary | Actual evidence |
|---|---|
| Thread context | Typed UUID, explicit removal, no implicit propagation into worker threads |
| JDBC binding | Positive A/B reads through primary production datasource; no-context select zero and insert 42501 |
| Pool return | Same backend PID reused without tenant context; unfinished writes roll back; aborted transaction recovers |
| Failure cleanup | Binding/reset/rollback failures evict the mock physical connection; no auto-commit after failed rollback |
| Hibernate | A/B assignment and save/clear/find without FoundationTenantConfiguration; unscoped write rejected 42501 |
| Request | Real filter-chain regression: unknown header initially reached authenticated route (200), now 401; active fixture succeeds |
| Resolution | Dev/test header; prod configured subdomain; malformed/duplicate/unknown input rejected; forwarded headers ignored |
| Global lookup | Actual baiti_migrator, no superuser/BYPASSRLS; active only, suspended/unknown absent, app pool remains baiti_app |
| Ownership | ArchUnit restricts context mutation to tenancy and forbids admin-plane domain repository access |

Final command: Invoke-NativeTests.ps1 with explicit private SettingsPath. Result:
**95 unit/architecture + 113 integration**, no failures/errors/skips, 43.126 s.
Earlier red logs capture the missing API, unbound JDBC context, missing Hibernate
identifier and unknown-tenant HTTP acceptance. One standalone sandbox unit run
stalled during Mockito attachment and was interrupted; the authorised native
runs include and pass those units. Intermediate compilation mistakes are retained
in ignored logs and are not counted as behaviour regressions.

The resolver must run before authentication so login can select a community;
JWT claim matching follows in 5b. Missing context uses Hibernate's unscoped
identifier while the database receives RESET and denies all domain rows. The
foundation fixture overrides only older mapping tests, explicitly ordered after
the production customiser. The global owner pool is annotated and bounded to the
current internal active-slug query; its existence does not prove super-admin
caller checks or onboarding.

Scoped author security review: **No blocking findings in reviewed scope**.
Reviewed bound SQL, role separation, cleanup/eviction, denial and host/header
cases. JWT authentication, refresh/CSRF, RBAC, actor auditing, async propagation,
onboarding rollback and full G-RLS remain unverified. No hosted CI, changed-line
coverage, push, PR, Docker or deployment claim.
