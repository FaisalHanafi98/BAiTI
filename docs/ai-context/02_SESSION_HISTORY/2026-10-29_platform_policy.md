# Explicit platform table policy — 29 October 2026

Owner authorisation: continue implementation and local commits after shared SOP,
Humanizer and checks, grouping related work into meaningful PRs. No publication
is authorised. Active local worktree: `backend/.local/persistence-acceptance`,
branch `test/persistence-acceptance`, based on runtime commit `781a401`.

ADR 0003 and database blueprint section 5 define the bounded policy contract.
The ordinary migrator failed both global insert positives under V29; focused
tests produced exactly two expected failures, no errors/skips. App denials were
already passing. Added forward V30, never rewriting earlier applied SQL.

V30 adds six policies, TO baiti_migrator only: SELECT/INSERT/UPDATE on tenants
and super_admins, with no DELETE/FOR ALL/PUBLIC/app expansion. FORCE RLS, ownership,
role attributes, domain policies and ledger grants remain. The old owner-denial
characterization is replaced by the intended role-specific positive contract;
app isolation assertions remain and now include existing-row canaries and actual
SET ROLE escalation denial. No HTTP authentication/service path is implemented.

## Actual evidence

- V29 fixture copied from the verified synthetic runtime target, preserving that
  source and all older databases; dedicated target:
  `baiti_test_acceptance_20261029_d9a1d543`.
- V29→V30 focused green: 58 unit + 9 integration cases, zero failures/errors/skips.
- A named synthetic platform admin was inserted through baiti_migrator, not the
  private fixture administrator. The expanded full suite passed 58 + 53 in
  33.264 seconds, zero failures/errors/skips.
- New target `baiti_test_acceptance_fresh_20261029_465b41b9` was verified empty.
  V1–V30 applied successfully; 58 unit + 5 migration cases passed.
- Both named tenant fixtures and the platform canary were then explicitly seeded
  through baiti_migrator. Full fresh suite passed 58 + 53 in 45.464 seconds,
  zero failures/errors/skips. Per-test writes roll back; shared named fixtures
  remain by design. Existing fixture identities and ordinary owner privileges
  are checked, without printing credentials or ownership tokens.

The [audit](../../audits/persistence-acceptance-2026-10-29/VERIFICATION.json)
records actual log hashes, old SQL preservation and remaining limits. The
platform owner still has trusted schema/DDL authority; DML DELETE denial does
not remove that ownership. V30 is transactional policy DDL with no backfill.

Next: remaining uniqueness/enum and privileged entity-mapping acceptance, then
P05. The separate platform principal/pool/onboarding, complete G-RLS, changed-line
coverage, hosted CI and release remain unimplemented. No Docker, real data,
role mutation, history repair/reset, push, PR or production operation occurred.
