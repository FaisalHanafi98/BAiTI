# P05 native exit — 8 November 2026

Work continued locally on `igumi`. Resident ownership, platform sign-in
and atomic onboarding, first-use credential replacement, and authentication
auditing landed as separate commits. The final native run passed 321 tests,
including I1–I9 and the permission matrix. The
[database evidence](../06_TESTING_EVIDENCE/2026-11-08_tenant_isolation.md)
records why V32 adds same-tenant foreign keys and V33 validates legacy rows.

The [dev smoke](../06_TESTING_EVIDENCE/2026-11-08_dev_authentication.md) used
two fictional communities on a separate owned PostgreSQL 16 database. Local
G-RLS evidence is complete. The hosted checks still need a GitHub run before
merge. Docker and hosted deployment remain deferred. The next backend work
is P08 property; P06 should use the native database contract when its CI is
prepared.
