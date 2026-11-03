# Authentication workflows — 30 October 2026

Branch feat/jwt-authentication, following a3f1e68. Work began 29 October; the final
native run on 30 October passed **140 unit/architecture + 136 integration tests**,
zero failures/errors/skips, in **51.630 seconds**. [Structured evidence](../../audits/jwt-authentication-2026-10-29/AUTH_FLOW.json)
records target identity, log hashes, expected red cases and the separate outage.
Raw logs, credentials, ownership tokens and runtime data remain ignored.

Real PG16 app-role fixtures exercise login, refresh, logout and me through the
Spring HTTP chain. Synthetic IDs are generated per test and removed by exact ID;
shared canaries remain. The suspended-community mutation rolls back. Password
verification uses bcrypt; refresh values have 256 random bits and only SHA-256
hashes persist. Rotation locks the row. A replay commits revocation independently
before returning 401, including concurrent rotation; inactive-user refresh revokes
all outstanding families. Logout also revokes all user refresh families. Already
signed access tokens retain their bounded lifetime; no denylist is claimed.

Cookie attributes: Secure, HttpOnly, SameSite=Strict, host-only, refresh path.
Cookie refresh also requires the allowed Origin and explicit X-BAiTI-Refresh
header; body fallback uses an explicit token without an ambient cookie. Only
login/refresh are anonymous. Credentialled CORS has no wildcard. Application auth
errors follow RFC 7807 and omit rejected credentials/query values; CORS protocol
rejections occur before that application handler. Required/invalid inputs,
multibyte bcrypt length, known/unknown/inactive/suspended failures and existing
JWT abuse/context checks pass. Auth bodies are bounded at 8 KB, including unknown
length reads. Rate tests cover IP and tenant/email budgets, unknown slugs, refill,
cache saturation and idle expiry. The cache is local: 20,000 keys, five-minute idle
expiry. New tokens honour shorter configured whole-second TTLs within v1's
15-minute access and seven-day refresh limits; cookies/response TTLs match.

Red evidence: login and refresh initially returned 401; the sixth login lacked
429; oversized JSON initially succeeded; errors had the temporary JSON shape;
the configurable lifetime API was absent. An intermediate native attempt failed
only at preflight after the private server stopped. pg_ctl confirmed the stopped
state and restarted the existing owned loopback cluster. Subsequent runs passed;
no database reset, role change or system-service/global PATH change occurred.

Scoped author review: **No blocking findings in reviewed public-authentication
scope**. This is server-side native integration evidence, not a browser/TLS/dev
curl demonstration, production audit or release clearance. RBAC/catalogue, actor
and authentication audit events, super-admin onboarding, async propagation, full
I1–I9 and changed-line coverage remain open. No Docker, hosting or deployment.

GitHub was checked read-only: one initial commit, 16d2621. The local repository
has no remote configured and nothing was pushed. Local commits follow the owner's
SOP/Humanizer direction; explicit push authorisation remains separate.
