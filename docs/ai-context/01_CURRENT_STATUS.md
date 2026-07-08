# BAiTI — Current Status

> **Last Updated**: 2026-07-08 — **P00 + P01 executed; repo live at commit `467d574`; session handed over** (see `ENGINEERING_HANDOVER_2026-07-08.md` and root `CURRENT_MODULE_STATE.md`). Next: P01 residue (Docker verify + folder rename), then packet P02.
> **Updated By**: Blueprint session (full SDLC planning & architecture programme)
> **Evidence Basis**: Direct inspection of every repository file, including a full read of `MyComunity 1.0 data model.pdf`. No terminal/database evidence possible (still pre-code).
> **Project name**: BAiTI (rename from MyComunity per `docs/audits/BAITI_LAUNCH_PROMPT.md`; folder rename still pending — gate G-NAME)

---

## Current Goal

Execute Packet **P00** (environment verification) from the new engineering blueprint, then proceed through the packet sequence P01 → P05 toward gate **G-RLS**.

## What Changed This Session (2026-07-07)

**The complete engineering blueprint now exists at `docs/blueprint/`** — nine documents + 19 AI implementation packets. It supersedes `MyComunity_PLAN.txt` and all older planning facts where they conflict (`docs/blueprint/00_REPOSITORY_INVENTORY.md §2`). Discovery corrected several defects in the old plan:

| Finding | Where fixed |
|---|---|
| PDF holds **23** tables, not 22; final schema = **26 relations** | blueprint 00 §3.1, 04 §2 |
| 8 additional PDF typos (incl. `premission_id`, `submitted at`, `update_at`, singular `block`/`notification`) | 04 §9 deviation register (complete list) |
| Migration ordering defect (`refresh_tokens` after RLS; numbering conflict §5.4 vs §8) | corrected sequence V1–V28, 04 §6 |
| **RLS would not have bound the table owner** — no migrator/app role split existed in any doc | 03 §6.2 dual roles + FORCE RLS + CI canary |
| `SET LOCAL` interceptor design was a no-op outside transactions | 03 §6.3 pool-safe `TenantAwareDataSource` |
| `payments.payment_method` free text vs `payment_methods` table | 04 §4.5 FK normalization |
| Missing SRS / use cases / access matrix / threat model / UAT / DR plans (2026-05-19 audit gaps) | blueprint docs 01, 02, 05, 06 |

## Current Blockers (updated 2026-07-07, second session pass — user approved all pending decisions)

| Blocker | Gate | Status |
|---|---|---|
| Dev environment | P00 | **DONE with one deferral** — Java 21 ✓, Maven 3.9.16 installed ✓, Node 24 ✓, git ✓, gh (FaisalHanafi98) ✓; **Docker daemon not starting — deferred by user**; required again at P02 (Testcontainers). Evidence: `docs/audits/PHASE0_VERIFICATION.md` |
| Folder rename → `baiti` | G-NAME | **APPROVED**; physical rename blocked by the running session's file lock — run after closing the session: `Rename-Item "...\Anep's Project (Baiti)" -NewName "baiti"` |
| Anep alignment | G-ANEP | **CLEARED** — `meeting/05_OUTCOME.md` (user confirmation) |
| GitHub repo location | G-PUSH | **DECIDED** — `FaisalHanafi98/baiti` private; actual push still needs explicit instruction |
| Deployment target | D3 | **DECIDED** — Fly.io SG (used at P18) |

## Current Code / Database / Testing State

**Zero lines of application code. No git repo. No database. No tests.** Unchanged — this session produced planning artifacts only, per its mandate.

## SDLC Phase Status (blueprint milestones)

| Milestone | Status |
|---|---|
| Blueprint (this package) | **DONE 2026-07-07** |
| M0 environment (P00) | NOT STARTED — next action |
| M1 foundation (P01–P03) | NOT STARTED |
| M2 security core (P04–P05, G-RLS) | NOT STARTED |
| M3 rails (P06–P07) | NOT STARTED |
| M4 v1.0 modules (P08–P12) | NOT STARTED |
| M5 hardening · M6 go-live · M7 v1.x | NOT STARTED |

## Next 5 Recommended Actions

1. **User**: confirm folder rename (G-NAME) and, if known, record the Anep meeting outcome to `meeting/05_OUTCOME.md` (G-ANEP).
2. Execute **P00** — run `java -version; mvn -v; node -v; npm -v; docker ps; git --version; gh auth status`, capture to `docs/audits/PHASE0_VERIFICATION.md`, install gaps.
3. Execute **P01** — repo bootstrap with the two-role DB init (this is where RLS enforceability starts).
4. Execute **P02–P03** — backend skeleton + all 28 migrations with the migration test.
5. Execute **P04–P05** — entities + the security/tenancy core, exiting at **G-RLS** with the isolation suite green.

## Do-Not-Repeat List (updated)

Everything from the previous list still applies (no PDF typos, no `git add -A`, no pushes, no auto-commits, no editing applied migrations, TDD always, no code in P00/P01) **plus**:
- Do NOT treat `MyComunity_PLAN.txt` migration numbering as current — `docs/blueprint/04_DATABASE_DESIGN.md §6` is the only sequence.
- Do NOT run the app and Flyway as the same DB role — dual-role rule (blueprint 03 §6.2).
- Do NOT start any feature module before gate G-RLS evidence exists.
- Do NOT create new artifacts under the MyComunity name.
