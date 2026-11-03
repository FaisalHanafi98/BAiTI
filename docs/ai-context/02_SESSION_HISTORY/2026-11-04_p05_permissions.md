# P05 permissions foundation — 4 November 2026

The owner instructed continued implementation after the planning intake, without
routine pauses. Work stays on igumi in the active publication checkout, based on
4ce314b. The original checkout's unrelated research and file moves are preserved;
main/anep are not advanced by this local author work. No new branch or push.

Implemented the 42-code tenant catalogue and immutable defaults for Admin,
Manager, Resident and Guard. A separately authored CSV fixture expands the
access grid's grouped codes; platform permissions remain outside the catalogue.
Named method security is enabled without introducing a second auth framework.

Permission sync is one scoped app-role transaction, serialised by an advisory
lock. Missing roles receive complete defaults; existing system roles receive
only defaults for newly created catalogue codes. Existing-code revocations,
custom roles and custom permissions are preserved. FR-RBAC-02, architecture and
the schema's template narrative record this reconciliation; no DDL changed.
Startup uses a tenant-directory interface and binds/clears each app transaction.
Test contexts disable its automatic invocation and exercise the service directly.

JpaConfig's empty auditor is replaced with a trusted authenticated TenantPrincipal
whose tenant matches TenantContext. Unauthenticated, unrelated or foreign/missing
context returns no auditor. Native tests prove persisted creator/editor values.
This does not add the separate async activity-log/auth-event implementation.

Meaningful observed red/green checkpoints: missing catalogue/default/startup/sync
APIs caused compilation failures; the existing actor stub returned an empty value
instead of the user UUID; a named method permission initially returned 200 when
403 was required, then passed after enablement. The first expanded native run had
one setup error: a synthetic tenant slug exceeded V2's 50-character check. Only
the fixture slug was shortened; discovery and the full suite then passed.

The sandbox could not observe the existing host PG process; a bounded start
attempt timed out. Elevated status found the original PID 36176. Approved native
runs reused that loopback cluster through the existing preflight; no reset,
provisioning, role mutation or applied migration edit occurred.

Final verification: 150 unit/architecture + 152 native integration tests, zero
failures/errors/skips; Maven reported 01:06 min. See
[evidence](../06_TESTING_EVIDENCE/2026-11-04_permissions.md). Concurrent committed
sync fixtures remove only the UUIDs they created; other writes roll back or have
exact fixture-owned cleanup. Source V1–V31 remains unchanged.

Scoped author review used Spring/backend/security/verification guidance. No
independent security clearance is claimed. Full P05/G-RLS is still open. Continue
with the resident ownership guard/404 contract, then platform onboarding and
async auth/audit/I-suite work. Docker, hosting and deployment stay deferred.
