# AI Context Archive — MyComunity

> **Created**: 2026-05-10
> **Project**: MyComunity (condominium/community management SaaS)
> **Working Directory**: `C:\Users\User1\Desktop\Career\Development\Anep's Project` (rename pending)
> **Archive Version**: 1.0.0

---

## Purpose

This archive is the AI-readable memory system for the MyComunity project. It enables any AI coding agent — Claude Code, Codex, OpenCode, or generic — to resume development from a verified, evidence-based state without relying on stale assumptions from previous sessions.

---

## How Future AI Agents Must Use This Archive

### On Session Start (or `/resume`)

Read in this order:

1. `CLAUDE.md` (project root — if it exists; else root Development/CLAUDE.md)
2. `docs/ai-context/README.md` (this file)
3. `docs/ai-context/00_PROJECT_SOURCE_OF_TRUTH.md`
4. `docs/ai-context/01_CURRENT_STATUS.md`
5. `docs/ai-context/08_DOC_ALIGNMENT/deviation_register.md`
6. `docs/ai-context/03_BUG_HISTORY/unresolved_bugs.md`
7. `docs/ai-context/03_BUG_HISTORY/failed_attempts.md`
8. Latest file under `docs/ai-context/02_SESSION_HISTORY/`
9. Then: `git status`, `git branch`, modified/untracked files
10. Then: verify against current repository evidence

### After Reading

Output a **Resume Report** before doing any work:

```
## Resume Report

Current Goal:
Current Blocker:
Current Repo State:
Current Database State:
Docs/Code Alignment Status:
Known Deviations:
Likely Valid Deviations:
Likely Mistakes:
Files Likely Involved:
Immediate Safe Next Action:
Actions Requiring User Confirmation:
```

---

## Update Rules

Every AI agent that completes meaningful work MUST update:

1. `docs/ai-context/01_CURRENT_STATUS.md` — always
2. Add a new file to `docs/ai-context/02_SESSION_HISTORY/` — always
3. Relevant bug history files — if bugs were found or fixed
4. Relevant module status files — if a module was changed
5. Testing evidence files — if tests were run
6. `docs/ai-context/04_DATABASE_STATE/` — if migrations changed
7. `docs/ai-context/08_DOC_ALIGNMENT/` — if docs/code drift was found

---

## Status Label Definitions

| Label | Meaning |
|---|---|
| **VERIFIED** | Confirmed by direct inspection: terminal, code, database, browser |
| **UNVERIFIED** | Stated in docs or prior summaries; not yet confirmed against current evidence |
| **INFERRED** | Strongly implied by surrounding evidence, but not directly proven |
| **PLANNED** | Described in docs as future intent; not implemented |
| **ALIGNED** | Docs and code agree |
| **DOC AHEAD OF CODE** | Docs describe something not yet in the code |
| **CODE AHEAD OF DOC** | Code implements something not documented |
| **STALE DOC** | Documentation describes a past state that no longer matches reality |
| **CONFLICTING DOCS** | Two or more docs disagree with each other |
| **DEVIATION** | Code diverges from the plan; cause unknown |
| **ACCEPTED DEVIATION** | Deviation is intentional and evidence-supported |
| **POSSIBLE DEFECT** | Code likely contains a mistake |
| **SUPERSEDED** | A plan or doc was replaced by a newer one |
| **UNKNOWN** | Not enough evidence to classify |

---

## Critical Rule

**Past session summaries are context, not proof. Existing docs are intent, not proof. Current code is reality, but not always correctness.**

When docs and code diverge: classify the deviation first. Do not auto-fix without user input.

Source-of-truth hierarchy:
1. Current repository files and actual code
2. Current database state and migration status
3. Terminal command output
4. Browser/Playwright verified behavior
5. CLAUDE.md
6. Existing project docs (after alignment audit)
7. `00_PROJECT_SOURCE_OF_TRUTH.md`
8. `01_CURRENT_STATUS.md`
9. `08_DOC_ALIGNMENT/`
10. Module status files
11. Bug history files
12. Past session summaries
13. Previous AI assumptions or chat memory
