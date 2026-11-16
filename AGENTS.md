# BAiTI — Agent Instructions (all AI tools)

This file carries the same authority as `CLAUDE.md` for non-Claude agents (Codex, OpenCode, Cursor, GLM, DeepSeek, Kimi, …). **Read `CLAUDE.md` — it is the single instruction source; this file only adds the resume steps.**

## Workstation and session handoff

For a new workstation, a fresh BAiTI session, or a request to research or
discover this folder, start with [SESSION_MIGRATION_PROMPT.md](SESSION_MIGRATION_PROMPT.md).
Follow its discovery order and consolidate the available context in one pass.
Keep planning and implementation in the canonical `baiti` checkout. Switch
Git branches in this folder when work needs a different branch. Use the steps
below for routine work in a known checkout.

## On session start

1. Read [../../standards/README.md](../../standards/README.md), `CLAUDE.md` (project rules) and `docs/ai-context/README.md` (resume protocol), then `docs/ai-context/01_CURRENT_STATUS.md`.
2. Find your work order in `docs/blueprint/08_ai_packets/` — packets execute in dependency order; the executor protocol in that folder's README is binding.
3. Verify, don't trust: `git status`, current files, and the latest `docs/ai-context/02_SESSION_HISTORY/` entry before acting.
4. On-demand workspace skills live in `.claude/skills/` (`springboot`, `react-ui-patterns`, `ui-accessibility`, `frontend-design-aesthetics`, `design-for-ai`, `impeccable`, `foreman`, `tdd`, `code-commenting`, `verification-before-completion`, `database-migrations`, `security-review`, `systematic-debugging`, `humanizer`). Trust their trigger descriptions and load them when relevant.

## Hard floor (even if you read nothing else)

- Schema comes only from `docs/blueprint/04_DATABASE_DESIGN.md` — never the PDF.
- Failing test first; isolation tests are sacred.
- Two DB roles (`baiti_migrator` / `baiti_app`) — never collapse them.
- No pushes, no auto-commits, no secrets, no edits to applied migrations.
- Update `docs/ai-context/` before you stop.
