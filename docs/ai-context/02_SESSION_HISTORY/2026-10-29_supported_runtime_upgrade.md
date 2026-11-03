# Supported runtime upgrade — 29 October 2026

Owner direction: continue implementation and checked local commits after the
shared SOP and Humanizer. Keep each PR meaningful. No push or GitHub publication
is authorised. Followed the existing Spring, security, migration, TDD and
verification guides; no subagents were delegated.

## Isolation and implementation

Foundation branch `test/native-postgres-harness` remains at `1aec31c`, preserving
earlier unrelated dirty research/file-organisation work. The managed worktree
tool rejected the nested repository path because the chat's registered workspace
is its parent Development folder. Created a normal Git worktree instead at
`backend/.local/runtime-upgrade`, branch `chore/supported-spring-baseline`.
No existing checkout or Git history was reset or removed.

ADR 0002 records the official support/migration sources and bounded decision.
Added a guard for the Boot version actually loaded at runtime. It failed on
3.3.5 as expected. Boot 3.5.16/springdoc 2.10.0 compiled as the documented bridge;
this was compile evidence, not a green integration milestone or final target.

Moved to Boot 4.1.2/springdoc 3.2.0, updated modular web/Flyway/test starters,
moved imports and explicit REST test configuration, and used the Jackson 3
parser for the JSON regression. ArchUnit uses its core artifact with ordinary
Jupiter methods. Removed unused container test dependencies; no container ran.
Kept Java 17 source compatibility on the existing Java 21 runtime.

## Actual verification

1. Old runtime guard: one expected assertion failure, no errors/skips.
2. Bridge compile and Boot 4 compile/format: BUILD SUCCESS, no test-run claim.
3. Initial existing-target full run: 58 unit/architecture + 45 integration green.
4. Added authenticated OpenAPI generation and anonymous-denial tests; full
   existing-target run passed 58 + 47, zero failures/errors/skips.
5. New owned target `baiti_test_runtime_20261029_855af26e` was verified to have
   zero public tables. The new Flyway applied all unchanged V1–V29 migrations;
   58 unit + 4 migration cases passed. Separate roles/ownership were checked.
6. Explicit private administrator setup inserted only the named synthetic A/B
   tenant fixtures. Full fresh-target run then passed 58 + 47 in 41.635 seconds,
   zero failures/errors/skips. Hibernate reports 7.4.13.Final.

The [structured runtime evidence](../../audits/runtime-upgrade-2026-10-29/VERIFICATION.json)
contains safe target identity, exact log hashes, unchanged SQL hashes and limits.
Passwords/ownership tokens stay in ignored ACL-restricted settings in the original
project. The runner loads only the seven test keys. No real data, reset, applied
migration edit, role change, remote audit, Docker, hosting or deployment occurred.

## Remaining gates

Test-only tenant resolution and a mock authenticated documentation user do not
prove production tenancy or authentication. P03/P04 still need their remaining
constraint/enum acceptance cases; platform provisioning requires an explicit
policy design. P05, I1–I9, changed-line coverage, normal production-profile
startup and hosted CI remain open. Preserve the foundation and runtime branches
as separate meaningful PR scopes when publication is explicitly authorised.
