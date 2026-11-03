# GitHub publication — 3 November 2026

Superseded later the same day by the owner's
[three-branch workflow](2026-11-03_three_branch_workflow.md). PRs #1–#5 are closed.
The stack below records the first publication, not the current branching model.

The owner requested pushing the existing BAiTI commits before further development.
GitHub main contained only initial commit `16d2621`, while the local authentication
branch held 23 commits through `19a7dce`. The histories had independent roots.

Configured origin and fetched main. A separate registered worktree at
`backend/.local/github-publication` joins the histories without changing any
original commit ID. The owner's MIT licence and project description are retained.
Every publication merge used the shared commit SOP and Humanizer guidance, with
staged secret and whitespace checks. No hooks were bypassed; none were configured.

The five branches were pushed together with an atomic, explicit-ref push. GitHub
API and remote-ref reads confirmed the published tips and draft PRs:

| Order | PR | Branch | Base |
|---|---|---|---|
| 1 | [Backend and native PostgreSQL foundation](https://github.com/FaisalHanafi98/BAiTI/pull/1) | `publish/native-postgres-foundation` | `main` |
| 2 | [Spring Boot 4.1.2](https://github.com/FaisalHanafi98/BAiTI/pull/2) | `publish/supported-spring-baseline` | foundation |
| 3 | [Persistence acceptance](https://github.com/FaisalHanafi98/BAiTI/pull/3) | `publish/persistence-acceptance` | runtime |
| 4 | [Tenant context](https://github.com/FaisalHanafi98/BAiTI/pull/4) | `publish/tenant-context` | persistence |
| 5 | [Authentication](https://github.com/FaisalHanafi98/BAiTI/pull/5) | `publish/jwt-authentication` | tenant context |

Review and merge in order using merge commits. The stack depends on that ancestry;
squash or rebase merging would require reconciling subsequent PR bases. No PR was
merged, and main remains at the owner's initial commit. No branch protection,
workflow, hosting or deployment setting was changed.

The full stack passed 276 tests today; see [evidence](../06_TESTING_EVIDENCE/2026-11-03_publication.md).
This is fresh native server/database evidence, not hosted CI or complete G-RLS.
The pre-publication high-signal scan covered 386 historical blobs with no matches
or unreviewed oversized objects. Private settings, binaries, data and raw logs
remain ignored. The original checkout's uncommitted research and file moves are
preserved locally and were not included in this committed-code publication.

Continue implementation in `backend/.local/github-publication` on
`publish/jwt-authentication`. Next is P05c RBAC/catalogue and actor auditing,
followed by onboarding, auth audit/async propagation and full I1–I9. Docker,
hosting/VPS and deployment remain deferred.
