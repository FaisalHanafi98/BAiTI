# Single BAiTI checkout — 17 November 2026

Faisal directed that BAiTI planning and implementation live together in the
`baiti` folder. Use Git branches in this checkout to separate work. Faisal
develops on `igumi`, Anep uses `anep`, and `main` is the production branch.

The original Git history was retained. All three local branches fast-forwarded
to their existing GitHub-tracking tips at `d5ed66d`, and `igumi` became the
active branch. The P07/P08 implementation and planning material are in its
working tree. Three stale registrations from the previous workstation were
pruned. Git now lists one worktree.

Compared backend and frontend files byte for byte before retiring the
duplicate checkout. Merged compatible planning edits and carried forward the
newer implementation guidance where documentation conflicted. Restored tracked
reference files from the implementation baseline where the planning checkout
had relocated older copies. The verified recovery ZIP under
`%LOCALAPPDATA%/BAiTI/recovery/` contains the original local variants, Git diffs,
branch tips and the previous frontend browser result. The duplicate project
folder was moved to the Windows Recycle Bin.

The [project overview](../../PROJECT_OVERVIEW.md) and
[session handoff](../../../SESSION_MIGRATION_PROMPT.md) now point to this
checkout. The [consolidation verification](../06_TESTING_EVIDENCE/2026-11-17_single_checkout.md)
records the source comparison, PostgreSQL suites and frontend checks. The next
product packet is [P09 People](../../blueprint/08_ai_packets/PACKET_09_people.md).
