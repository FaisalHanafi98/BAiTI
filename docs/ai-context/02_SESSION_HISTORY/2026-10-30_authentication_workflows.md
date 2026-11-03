# Authentication workflow checkpoint — 30 October 2026

Implemented native password login and opaque refresh rotation/replay handling,
logout/me, explicit cookie origin/header checks and CORS, IP/account limits,
bounded auth request bodies and the blueprint's RFC 7807 error contract. Token
TTL environment values now bind to validated whole-second lifetimes within the
published v1 limits. Only hashes persist for refresh credentials. No SQL/role change.

Final verification: 276 tests (140 units/architecture + 136 integration), zero
failures/errors/skips. Concurrent rotation, inactive/suspended/unknown failures,
validation privacy, cache bounds and JWT/context regressions pass. An intermediate
native-server outage failed preflight; only the existing private PG16 cluster was
restarted and reruns passed. See [evidence](../06_TESTING_EVIDENCE/2026-10-30_authentication_workflows.md).

Next: P05c catalogue/RBAC and actor auditing; then privileged onboarding, auth audit
and async propagation, complete I1–I9 and normal dev journeys. Full G-RLS, changed-line
coverage and hosted/release gates remain open. Docker/hosting stay deferred.
GitHub still has only the owner's initial commit. No remote/push/PR; the owner's
local-commit instruction does not authorise publication.
