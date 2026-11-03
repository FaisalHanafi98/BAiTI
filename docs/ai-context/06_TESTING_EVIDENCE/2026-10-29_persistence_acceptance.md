# P03/P04 local persistence acceptance — 29 October 2026

Branch: test/persistence-acceptance, following platform policy commit 1fa8535.
Command: Invoke-NativeTests.ps1 with explicit private settings for owned PG16.
[Structured evidence](../../audits/persistence-acceptance-2026-10-29/P04_ACCEPTANCE.json)
records target names and log hashes; credentials and raw logs remain ignored.

| Requirement | Evidence |
|---|---|
| All 26 relations map and schema validates | Existing aggregate smoke suite + new privileged Tenant/SuperAdmin JPA tests |
| Every authored business unique index | 13 tenant cases + 2 global cases; catalog inventory fails if an index is unclassified |
| Composite role grant key | Duplicate grant rejected, SQLSTATE 23505 |
| All 16 enum fields | Every Java value persists and refreshes with its matching DB string; reflective inventory guards additions |
| Partial unique indexes | MOVED_OUT/null-user resident and CANCELLED/REJECTED booking positive cases |
| Invalid authored checks | Family ages, bill amount/month, payment insert amount, event and visitor ordering rejected |
| Timestamp auditing | Save/clear/find tests, including global entities on explicit test-only owner pool |
| Lazy associations | Reflective ArchUnit companion checks every mapped relationship annotation |
| Every FK has a leading index | Catalog test failed with six missing relationships, then passed after forward V31 |
| Actual app role and forced RLS | Existing preflight, HealthIT, native boundary and platform canary suites retained |
| Fresh/upgrade | V30→V31 full suite; separate empty V1–V31 MigrationIT before explicit fixture setup, then full suite |

Final full results on both targets: **59 unit/architecture + 100 integration**,
zero failures/errors/skips. The fresh full run took 37.165 seconds. Fresh migration
run: 59 unit/architecture + 6 migration tests. All V1–V30 source bytes are
preserved; V31 contains six ordinary transactional CREATE INDEX statements.
Existing databases and synthetic fixtures were retained; test transactions roll back.

The global JPA pool is test-only and asserts the actual baiti_migrator principal;
normal app tests assert baiti_app. No production platform pool is inferred.
Constraint acceptance does not implement service state transitions. Actor
AuditorAware is still a P05 stub. Full I1–I9, changed-line coverage, hosted CI and
production locking/performance remain unverified. No push, PR or deployment.
