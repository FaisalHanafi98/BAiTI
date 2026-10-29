# Native foundation checkpoint — 29 October 2026

The owner now authorises continued local commits after drafting with the shared
commit SOP, applying Humanizer and running the relevant checks. Per-message
approval pauses are superseded for this work. Related code, tests and documents
belong in meaningful PRs; no push or remote history change is authorised.

## Committed source and verification

| Commit | Outcome | Native evidence |
|---|---|---|
| 3c78d92 | Existing V18 actor correction and schema alignment | Prior clean-export baseline recorded |
| cb2399d | Native target preflight, runner, startup mappings and migration-resource guard | Clean export: 46 unit + 10 integration, all green; empty target applied V1–V28 |
| e2bb68c | Explicit tenant/audit permission link and aggregate/architecture regressions | Clean export: 57 unit + 24 integration, all green |
| aa126eb | Forward V29 setting parser/policies and raw boundary tests | Clean export: 57 unit + 38 integration, all green; V28→V29 retained two synthetic tenants and a separate sentinel |
| 2602fc4 | Complaint cascade/orphan history protection and platform characterization | Two expected behavioural failures before repair; final 57 unit + 45 integration, zero failures/errors/skips |

Runtime: native PostgreSQL 16.15, Java 21.0.11, Maven 3.9.16. The fresh ordered
commit target is `baiti_test_commit_20261029_2f918711`, loopback 55432, with
separate app/migrator roles and a private ownership marker. Named fixtures and
the sentinel are synthetic. Earlier test databases were preserved. The private
cluster remains running; credentials/binaries/data and raw logs stay ignored.

The [structured audit](../../audits/foundation-2026-10-29/VERIFICATION.json)
records actual counts/log hashes and all 37 protected baseline SQL/Anep files.
Static migration typo search returned no matches; explicit EAGER search returned
no matches. That search alone is not runtime lazy-loading proof. Staged whitespace
and the canonical staged-blob secret scan returned zero before each source commit.
No active Git hooks were configured or bypassed. One test attempt stopped at
Spotless before execution; its required formatting was fixed before the green run.

## Interpretation and next bounded work

This closes the named F0 characterization/repair checkpoint with explicit
remaining gaps, not full P03/P04 acceptance. See the
[acceptance mapping](../06_TESTING_EVIDENCE/2026-10-27_native_harness.md).
Global inserts by app and ordinary migrator are confirmed denied under FORCE
RLS; the real platform policy/onboarding path remains absent. Test-only fixed
tenancy, selected constraint tests and raw boundary probes do not close P05,
I1–I9, exhaustive constraint/enum coverage, changed-line coverage or hosted CI.

The supported-version review confirmed Boot 3.3 is past open-source support.
[ADR 0002](../../adr/0002-supported-spring-baseline.md) scopes an isolated runtime
upgrade before authentication. Preserve this foundation branch and its verified
source. No Docker, hosting, production operation, real-data operation, push,
GitHub PR or repository-visibility change occurred.
