# P05 slice 5a checkpoint — 29 October 2026

Production TenantContext now drives JDBC session binding and Hibernate tenant
assignment. A one-connection PostgreSQL test proves return cleanup, including
unfinished/aborted transactions; failure-path units verify eviction. Request
resolution, profile-selected headers/subdomains and finally cleanup are wired
into the existing security chain. An annotated owner-role pool provides the
parameterised active-slug lookup; the primary app path remains baiti_app.

Red checks reproduced unbound JDBC reads, missing Hibernate repository context
and unknown tenant acceptance through the real HTTP chain. Final verification
passes 208 tests (95 units/architecture + 113 integration), zero failures/errors/
skips. [Evidence](../06_TESTING_EVIDENCE/2026-10-29_tenant_context.md) records scope,
log hashes, the interrupted standalone Mockito attempt and remaining limits.
No DDL changes. Native/Docker deferral and owner local-commit direction retained.

Active branch feat/tenant-context at the persistence-acceptance worktree. Next:
5b JWT authentication and refresh rotation, then RBAC, platform onboarding,
async context and the full I1–I9 suite. The lookup seam is not a super-admin
caller boundary; mock-user HTTP evidence is not a login demonstration.
