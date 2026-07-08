# BAiTI — Agent Instructions (all AI tools)

This file carries the same authority as `CLAUDE.md` for non-Claude agents (Codex, OpenCode, Cursor, GLM, DeepSeek, Kimi, …). **Read `CLAUDE.md` — it is the single instruction source; this file only adds the resume steps.**

## On session start

1. Read `CLAUDE.md` (project rules) and `docs/ai-context/README.md` (resume protocol), then `docs/ai-context/01_CURRENT_STATUS.md`.
2. Find your work order in `docs/blueprint/08_ai_packets/` — packets execute in dependency order; the executor protocol in that folder's README is binding.
3. Verify, don't trust: `git status`, current files, and the latest `docs/ai-context/02_SESSION_HISTORY/` entry before acting.

## Hard floor (even if you read nothing else)

- Schema comes only from `docs/blueprint/04_DATABASE_DESIGN.md` — never the PDF.
- Failing test first; isolation tests are sacred.
- Two DB roles (`baiti_migrator` / `baiti_app`) — never collapse them.
- No pushes, no auto-commits, no secrets, no edits to applied migrations.
- Update `docs/ai-context/` before you stop.
