# BAiTI — Alignment Outcome (gate G-ANEP resolved)

> **Recorded**: 2026-07-07
> **Basis**: User (Faisal) approval given in the blueprint session — "yes to all of the questions". This supersedes the UNVERIFIED status that `docs/ai-context/` and the 2026-05-19 audit attached to the 2026-04-08 meeting outcome.

## Confirmed decisions

| # | Question (from 01_MEETING_BRIEF.md / blueprint decision register) | Outcome |
|---|---|---|
| 1 | Multi-tenant SaaS from day 1? | **Yes** — as designed in the blueprint |
| 2 | MVP module split (auth/RBAC, property, residents, finance, complaints+maintenance first)? | **Yes** — SRS MoSCoW priorities stand (ASM-2 discharged) |
| 3 | Stack (Spring Boot 3 / Java 17 / PostgreSQL 16 / React 18 + TS + Vite)? | **Yes** — ratified |
| 4 | Collaboration: Faisal backend lead, Anep frontend lead, PR review both ways? | **Yes** — as proposed |
| 5 | Repo location (D2) | **`github.com/FaisalHanafi98/baiti`, private** (recommended option accepted). G-PUSH note: repo creation/push still requires an explicit instruction at that moment (CON-4) |
| 6 | Deployment target (D3) | **Fly.io, Singapore region** (recommended option accepted) — executed at P18/M6 |
| 7 | Project name / folder (G-NAME) | **BAiTI / `baiti`** confirmed. Physical rename pending: the folder was locked by the running session; run after closing it: `Rename-Item "C:\Users\asbou\Desktop\Work\Development\Anep's Project (Baiti)" -NewName "baiti"` |
| 8 | Billing currency / language | MYR-only + currency column; EN-only, i18n-scaffolded (blueprint baselines D4/D5 confirmed) |

## Caveat

These confirmations were given by Faisal. If Anep later raises a differing preference (frontend stack, ownership, scope), reopen the affected decision via a session note here and an update to the blueprint decision register (`docs/blueprint/00_REPOSITORY_INVENTORY.md §5`) — frontend packets (P07+) are the earliest point where his input is load-bearing.

**Gate status: G-ANEP OPEN → CLEARED. Frontend packets are unblocked.**
