# Native tenant isolation and reference integrity — 8 November 2026

PostgreSQL 16.15 ran at `127.0.0.1:55432` with separate migrator and app
roles. The native runner verified the exact ownership marker before using
`baiti_test_p04_20261029_bb8f567e`. It upgraded the retained V31 database,
and the full final run passed 151 unit/architecture and 170 integration
tests with zero failures, errors or skips. I1–I8 ran in
`TenantIsolationIT`, and I9 ran in `PlatformAuthIT`. The full run also
included the role/permission matrix.

The fresh target `baiti_test_v32_20261108_09bfa771` started with an empty
public schema. Preflight first required its schema owner to be set explicitly
to `baiti_migrator`. After that setup, Flyway applied V1–V33 in 2.081 seconds
and all seven `MigrationIT` tests passed.

The first I3 run exposed a real database gap. The test-only write route
accepted a foreign block when the repository used a reference by ID.
The route now resolves the block within the caller's scope. V32 also
enforces the same-tenant relationship for direct SQL on every existing
domain foreign key. The catalog checks all 28 constraints, their supporting
indexes and both column-specific `SET NULL` actions.

V32 blocked new bad writes but its installation as the FORCE RLS owner
accepted a legacy invalid row in a recovery drill. In a second drill on
the separate owned V31 database, a transaction inserted a synthetic invalid
reference and executed V32 and V33. V33 failed with SQLSTATE `23503`
at `units_block_id_fkey`. After the transaction closed, the database
was still at V31 and neither the synthetic rows nor V32 objects remained.
The migrations preserve the existing database policies, privileges and data.
Repair any detected legacy references under a separate approved plan.

The synthetic onboarding baselines remain in the owned test database.
Run-specific rows are removed by ID or the test request address. Raw
output is in the ignored `docs/audits/isolation-2026-11-08/` directory.
See the [case list and repeat command](../../security/RLS_TEST_STRATEGY.md).
