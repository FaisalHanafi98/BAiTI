# Permission and actor verification — 4 November 2026

Scope: catalogue/defaults, scoped sync/startup, method authorities, endpoint
classification and persisted actor stamps on igumi, based on 4ce314b. This is a
partial P05c checkpoint, not full P05/G-RLS or a working business module.

Executed from the active checkout using the established ignored P04 settings:

```powershell
./tools/testing/Invoke-NativeTests.ps1 -SettingsPath <existing-private-p04-settings>
```

The runner's seven-key environment load, exact synthetic target/ownership and
role preflight are unchanged. Runs required the execution context that can see
the existing loopback PostgreSQL 16.15 server. No database was reset and no role
or migration was changed. V1–V31 source files match the pre-change commit.

| Final full run | Tests | Failures | Errors | Skips |
|---|---:|---:|---:|---:|
| Unit / architecture | 150 | 0 | 0 | 0 |
| Native integration | 152 | 0 | 0 | 0 |
| Total | 302 | 0 | 0 | 0 |

Maven and Surefire/Failsafe XML agree. Exit 0, BUILD SUCCESS, reported time
01:06 min. The ignored log is `docs/audits/rbac-2026-11-04/23-permission-full-suite.log`;
hashes and staged checkpoints are in the
[structured record](../../audits/rbac-2026-11-04/VERIFICATION.json).

Assertions cover exact catalogue/module and default-role parity; no platform
defaults; native app-role bootstrap; stable IDs and preserved revoked grants;
new-code-only grants on existing system roles; untouched custom roles/permissions;
missing-context denial; rollback; A/B permission-ID isolation; concurrent committed
sync without duplicates and fixture-owned cleanup. Startup tests check context
cleanup on success/failure, rejection of an already-scoped caller and the test
toggle. Native owner discovery includes a rolled-back suspended community.

The method matrix performs 168 permission decisions (42 codes × four roles) on
an explicitly labelled test-only probe using signed tenant JWTs. An Admin with
no named authority is denied. Handler enumeration and generated OpenAPI must
match the four classified application endpoints; newly hidden/unclassified
application mappings are still caught by the handler check. This does not prove
authorisation of future feature endpoints or actual onboarding-generated roles.

Actor tests reject unrelated/unauthenticated/foreign identities and stale cleared
contexts. Native persistence keeps the original creator and updates the editor.
Anonymous/system auditor absence is not a claim that activity events or platform
actor semantics are implemented.

Red stages include missing APIs, the empty actor stub and 200 instead of required
403 before method security was enabled. An intermediate 21-IT batch had one
fixture error from an overlong slug; its correction passed discovery and the
final full suite. Do not classify that setup error as a production code failure.

The bounded scanner and source review do not establish a general security
clearance. Resident own-scope, platform identity/onboarding, async auth/audit,
complete I1–I9, browser/curl journeys, changed-line coverage, hosted CI and release
readiness remain open. No push, PR, Docker or hosted deployment is claimed.
