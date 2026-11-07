# Resident ownership verification — 8 November 2026

The native runner reused the owned PostgreSQL 16 test target. Its first attempt
failed preflight with SQLSTATE 08001 because the cluster was stopped. After
starting the existing loopback cluster, the same-tenant denial returned HTTP
404 with no content type. The scoped exception handler now emits the standard
problem JSON with the neutral detail `Resource not found`.

The full native `Invoke-NativeTests.ps1` run passed 151 unit/architecture and
156 integration tests, with zero failures, errors or skips in 01:23. Test-only
routes exercise owner access, missing permission, a second resident in the
same community, a foreign community, unknown IDs, active units, moved-out
history and unlinked portal accounts. Fixture teardown removes its own rows.
The raw logs are in the ignored `docs/audits/own-scope-2026-11-04/` directory.

These results cover the shared ownership mechanism. P05d onboarding and the
full I1–I9 gate remain ahead.
