# P05 access-token boundary — 29 October 2026

Branch feat/jwt-authentication, base 91d6f61. Boot-managed Spring Security JOSE
signs/verifies HS256 access tokens. No remote issuer/JWK discovery is configured.
Main startup requires JWT_SECRET as Base64 of at least 32 random bytes. Native
tests generate an independent per-process in-memory key. No secret, raw token
or real personal data is included in committed evidence.

[Structured record](../../audits/jwt-authentication-2026-10-29/VERIFICATION.json)
contains actual loaded library names, native target identity and local log hashes.
Full native command passed **119 unit/architecture + 119 integration tests**, zero
failures/errors/skips; Maven reported 01:03 min. No SQL or role changes.

The initial API compilation failed before implementation. The expiry regression
then proved a token was accepted at exactly 15 minutes; the explicit strict expiry
check closes that boundary. A real-chain regression proved the skeleton returned
403 for a valid bearer token; matching signed tenant access now returns 200.

Tests reject wrong signatures, changed tenant payloads, alg:none, HS512, missing
claims, malformed IDs/authorities, wrong issuer/audience/purpose, future tokens
and oversized input. HTTP tests check missing/foreign tenant context, expired/
malformed/duplicate auth headers, tenant-token platform denial (403), no session
creation and no authentication carried into the next request. Library imports
remain restricted to the security package. Generated-user/basic/form/session
fallback is disabled; anonymous health remains available.

Scoped author review: **No blocking findings in reviewed access-token scope**.
The HTTP suite signs synthetic principals directly. This does not establish
password login, refresh-family reuse handling, logout, me, rate limits, cookie
CSRF/CORS, actor auditing or the complete super-admin plane. CSRF is disabled for
the current bearer-only surface; cookie refresh must add origin/header protection
in its own implementation. Full G-RLS, changed-line coverage, hosted CI, production
key rotation and deployment remain unverified. No push/PR/Docker/hosting work.
