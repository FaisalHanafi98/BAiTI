# Publication verification — 3 November 2026

Scope: the five-branch publication stack through `2dfaff1`, whose backend,
migrations, tools and infra are identical to original authentication commit
`19a7dce`. Subsequent publication handoff edits affect documentation only.

Executed from the publication checkout:

```powershell
./tools/testing/Invoke-NativeTests.ps1 -SettingsPath <private-ignored-p04-settings>
```

The existing owned PostgreSQL 16.15 loopback cluster was stopped. Its exact data
directory and PG_VERSION were checked before restarting it with a hidden pg_ctl
helper on port 55432. No initdb, database reset, role change, Windows service,
Docker or global PATH change occurred. The established synthetic V31 test target
and separate baiti_app/baiti_migrator roles were reused.

Surefire and Failsafe XML totals independently agree with Maven's summary:

| Suite | Tests | Failures | Errors | Skips |
|---|---:|---:|---:|---:|
| Unit / architecture | 140 | 0 | 0 | 0 |
| Native integration | 136 | 0 | 0 | 0 |
| Total | 276 | 0 | 0 | 0 |

Exit code 0, BUILD SUCCESS, reported Maven time `01:23 min`. The ignored raw log
is `docs/audits/publication-2026-11-03/01-native-verify.log`, SHA-256
`b339feb52f200de9c1cf03b0e9b26b20f172938b20769ced4d7b84fc3253fb16`.
The [structured record](../../audits/publication-2026-11-03/VERIFICATION.json)
records publication and scan scope without credentials or raw tokens.

The canonical high-signal history scan checked all 386 blobs reachable from the
23-commit local implementation and the independent GitHub initial commit. There
were no pattern matches, unsupported oversized blobs or tracked private env/local
runtime/build paths. Staged scans and whitespace checks covered the publication
merges. This bounded scanner does not prove the absence of every possible secret
or establish a general security clearance.

Remote refs and GitHub's API confirmed all five publication branches and draft
PRs #1–#5. Both original histories are ancestors of the authentication branch;
no force-push or original-commit rewrite occurred. Main was not changed.

Limits: only the final combined stack was rerun today. Earlier PR-specific fresh
install and upgrade results retain their original dates. RBAC, actor/auth audits,
onboarding, async propagation, complete I1–I9, browser/curl development journeys,
changed-line coverage, hosted CI and release readiness remain open. No Docker,
hosting/VPS or deployment claim is made.
