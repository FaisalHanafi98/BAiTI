# Platform onboarding verification — 8 November 2026

The existing native PG16 target passed its role and ownership preflight.
Platform login initially returned 401 for valid credentials. After adding its
chain, focused authentication and permission tests passed. The authenticated
onboarding request then reproduced a missing route with HTTP 404.

The full `Invoke-NativeTests.ps1` run passed 151 unit/architecture and 160
integration tests with zero failures, errors or skips in 01:13. Tests cover
platform sign-in, the access-token audience boundary in both directions, account
suspension, page size, complete seeds, duplicate slug and step-four rollback.
I-9's injected encoder failure observes the already seeded roles and catalogue
before throwing. A separate transaction checks all affected tables for absence.
Successful creation tests use a rolled-back operator fixture.

The API accepts a caller-supplied initial password because the original sequence
left credential delivery unspecified. It returns only the tenant and admin IDs.
The password-change flag is stored. First-use enforcement and the remainder of
the isolation gate are subsequent work. No schema migration was required.

Raw red/green logs are in the ignored `docs/audits/platform-2026-11-08/` directory.
