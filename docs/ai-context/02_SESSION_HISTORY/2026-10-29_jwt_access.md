# P05 access-token checkpoint — 29 October 2026

Implemented HS256 tenant access-token signing/verification, strict claim and expiry
validation and a stateless bearer filter after tenant resolution. Main JWT_SECRET
is required; native tests generate their own ephemeral in-memory key. The default
generated user and form/basic/session fallback are disabled. A valid tenant token
cannot reach platform routes. No DDL changes.

The exact-expiry test failed before its strict boundary fix; real-chain bearer
access also failed before wiring. Final native verification: 238 tests (119
units/architecture + 119 integration), zero failures/errors/skips. See
[scope evidence](../06_TESTING_EVIDENCE/2026-10-29_jwt_access.md).

Next is real password login and opaque refresh rotation/reuse revocation, then
logout/me, rate limits and cookie/CORS tests. RBAC, actor auditing, onboarding,
async isolation and full I1–I9 still follow. Native execution and SOP/Humanizer
local commits remain authorised; no push, PR or deployment.
