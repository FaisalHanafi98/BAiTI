# ADR-0001 — Project Naming: BAiTI

**Status**: Accepted (user-confirmed 2026-07-07) · **Context date**: rename first mandated 2026-05-19 (`docs/audits/BAITI_LAUNCH_PROMPT.md`)

## Context

The project was planned as "MyComunity" (single-m, after the source data-model PDF). On 2026-05-19 it was renamed **BAiTI**. The working folder was still `Anep's Project (Baiti)` and no code/package/repo names existed yet — the cheapest possible moment to fix naming for good.

## Decision

| Artifact | Name |
|---|---|
| Project | **BAiTI** |
| Folder | `baiti` (rename approved 2026-07-07; execution pending a session file-lock — run `Rename-Item "…\Anep's Project (Baiti)" -NewName "baiti"` after closing the locking session) |
| GitHub repo | `github.com/FaisalHanafi98/baiti`, **private** |
| Java package root | `my.baiti` |
| Application class | `BaitiApplication` |
| Prod subdomain pattern | `{tenant-slug}.baiti.app` (domain purchase TBD at P18) |

Legacy files (`MyComunity_PLAN.txt`, `MyComunity 1.0 data model.pdf`, pre-2026-07 `docs/ai-context/` content) keep their names for traceability. Every artifact created from bootstrap onward uses BAiTI naming; a new file bearing the old name is a review-rejection.

## Consequences

- No code-level rename debt will ever exist (rename landed before line 1 of code).
- Historical docs mention MyComunity; readers are redirected by `docs/blueprint/00_REPOSITORY_INVENTORY.md §3.5`.
