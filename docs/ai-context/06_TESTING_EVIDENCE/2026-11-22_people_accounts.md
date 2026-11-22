# People account API verification — 22 November 2026

Verified on `igumi`, based on `0f8737d`, with the P09 account administration
slice in the working tree. Java 21 ran Maven with Java 17 source compatibility.
PostgreSQL 16.15 used the existing marked, owned loopback database
`baiti_test_consolidation` on port 55439. Runtime and migration connections
retained their separate `baiti_app` and `baiti_migrator` roles.

The final serial command was the existing `tools/testing/Invoke-NativeTests.ps1`
with `-SettingsPath` pointing to the private verification settings. Its
[local output](../../../backend/.local/p09-final-serial.log) ended with
`BUILD SUCCESS`. Counts were 151 unit/architecture tests and 190 integration
tests, with zero failures, errors or skips. The 11 People cases cover creation,
restricted first-use login, pagination/sort validation, role choices, assignment,
refresh revocation, idempotent deactivation, audit effects, input/duplicate
failures, named permissions, foreign resources and existing OpenAPI operation IDs.
The full tenant isolation and permission matrix suites passed.

Red/green observations were captured in ignored local logs. Creation initially
returned 404. The list returned 405 and role assignment returned 404 before their
implementations. The contract regression observed `list_1` replacing `list` on
the existing unit endpoint before the new controller methods received distinct
names. Final OpenAPI comparison found zero changed existing operations or schemas.
The generated TypeScript contract was refreshed using `npm run generate:api`.

JaCoCo executed 120 of 123 changed executable Java lines, or 97.6%, above the
70% changed-line floor. Existing SQL migration files have an empty diff.
The initial Maven attempt selected Java 8 and stopped at the formatting plugin.
Selecting the installed Java 21 runtime resolved that environment issue. The
lifecycle fixtures use separate synthetic IPs to preserve the real rate limit.
An overlapping compile caused one intermediate run to discover only 88 unit
tests. The final serial run above rediscovered all 151 and completed normally.

The frozen artifact adds six operations on five People paths. Read-only role
choices accept `USER_MANAGE` or `ROLE_MANAGE`. Account writes require
`USER_MANAGE`. Temporary passwords are generated, hashed, returned only on
creation, excluded from later reads, redacted from response string conversion
and protected by `Cache-Control: no-store`. Account writes and their audits
share a transaction. Existing bearer claims retain their signed expiry.
