# Three-branch workflow — 3 November 2026

Status: current owner direction. Replaces the five stacked publication branches
and draft PRs created earlier today.

The owner requested one production branch and two author branches, named igumi
and anep, with all three aligned now and the other branches deleted.

| Branch | Purpose | Starting point |
|---|---|---|
| `main` | Production branch and PR target | Complete published implementation plus this workflow record |
| `igumi` | Faisal's development branch | Same commit as main |
| `anep` | Stakeholder's development branch | Same commit as main |

Keep these three branches. Commit coherent changes on the relevant author branch
using the shared SOP and Humanizer guidance. Open a meaningful PR from that
author branch to main when its change is ready. Do not create extra feature,
test, runtime or publication branches without owner direction. The two author
branches can diverge during development; today's alignment is a starting point,
not permission to overwrite either person's later commits.

The owner explicitly authorised advancing main and aligning both author branches
for this consolidation. The update preserves all ancestors of published commit
`042bc16`, including the 23 original local commits and GitHub initial `16d2621`.
No history rewrite, hard reset, force-push or deployment is part of this change.

Draft PRs #1–#5 were closed instead of merging the superseded stack. Obsolete
branch tips are checked as ancestors of the retained implementation before their
references are removed. The original and older test/runtime worktrees are kept
at their existing commits with detached HEADs; no worktree or file is deleted.
The original checkout's 84 local changes and tracked diff hash matched before
and after detachment. The active implementation worktree remains
`backend/.local/github-publication`, now on `igumi`.

Verification for this change is branch ancestry, exact local/remote reference
equality, unchanged implementation trees, local documentation links and staged
secret/whitespace checks. The full native suite passed 276 tests earlier today;
see [that run](../06_TESTING_EVIDENCE/2026-11-03_publication.md). It is not rerun
for Git-reference and documentation changes. Remote alignment is checked after
the atomic push; only main, igumi and anep should remain.

RBAC, auditing, onboarding, full I1–I9, hosted CI and release readiness remain
unfinished. Naming main as the production branch does not claim those gates or
a live deployment. Docker, hosting/VPS and deployment remain deferred.
