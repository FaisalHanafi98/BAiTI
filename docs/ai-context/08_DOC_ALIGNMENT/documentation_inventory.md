# Documentation Inventory

> **Last Updated**: 2026-05-10
> **Inspected By**: AI Context Archive initialization (2026-05-10)

---

## Documents Found in This Project (`Anep's Project/`)

| Document Path | Purpose | Appears Current? | Notes |
|---|---|---|---|
| `MyComunity_PLAN.txt` | Full SDLC plan — stack, schema, architecture, CI/CD, roadmap, risks, open questions | YES | Last updated 2026-04-08. Comprehensive 527-line plan. Primary source of truth. |
| `meeting/01_MEETING_BRIEF.md` | Prep document for Anep alignment meeting (2026-04-08) | YES (historical) | Meeting-prep doc; its value is the decisions captured. Outcome of meeting is UNVERIFIED. |
| `meeting/02_STACK_RATIONALE.md` | Justification for Spring Boot + PostgreSQL + React stack | YES | Aligned with plan. Reference doc for explaining stack choices. |
| `meeting/03_SPRINGBOOT_REFRESHER.md` | Spring Boot concepts translated from Laravel perspective | YES | Reference doc — valid ongoing. Not tied to a specific implementation state. |
| `meeting/04_REACT_REFRESHER.md` | React 18 concepts and patterns | YES | Reference doc — valid ongoing. |
| `MyComunity 1.0 data model.pdf` | Original 22-table schema from Anep/source | YES (with caveats) | Contains typos (documented in plan). Plan's corrections take precedence. |

---

## Documents Found in `.orchestrator/` (Root Development Folder)

| Document Path | Purpose | Appears Current? | Notes |
|---|---|---|---|
| `.orchestrator/sessions/MYCOMUNITY_SESSION_2026-04-01.md` | Session init file for pre-stack-decision session | STALE | Created 2026-04-01, states "stack not decided yet" — superseded by 2026-04-08 plan which confirms Spring Boot stack. |
| `.orchestrator/subagents/PORTFOLIO_INIT.md` | Portfolio project context | N/A | Different project |
| `.orchestrator/subagents/CREAMS_INIT.md` | CREAMS project context | N/A | Different project |
| `CLAUDE.md` (root) | Root SOP v2.2.0 governing all projects | YES | 891 lines. No MyComunity-specific section. No AI context archive SOP. |

---

## Documents NOT Found (But Should Exist)

| Missing Document | Why Needed | Action |
|---|---|---|
| `CLAUDE.md` (project-level) | Project-specific SOP extension — plan lists this as a critical file to create | Create in Phase 1 |
| `README.md` | Project overview, setup, contribution guide | Create in Phase 1 |
| `.gitignore` | Java + Node + IDE + secrets exclusions | Create in Phase 1 |
| `.env.example` | Documented env var template | Create in Phase 1 |
| `docs/adr/0001-schema.md` | Schema decision record (PDF typo corrections) | Create in Phase 3 |
| `docs/adr/0002-auth.md` | JWT + RBAC + tenancy design record | Create in Phase 5 |
| `infra/docker-compose.yml` | Local postgres + pgadmin | Create in Phase 1 |
| `backend/pom.xml` | Maven build file | Create in Phase 2 |
| Any Flyway migration | Database schema | Create in Phase 3 |

---

## Root-Level Documentation (Sibling Projects — Context Only)

Not inspected in detail for this archive. Documented for routing awareness:

| Project | CLAUDE.md | Init File |
|---|---|---|
| CREAMS (Laravel) | `CREAMS/CLAUDE.md` | `.orchestrator/subagents/CREAMS_INIT.md` |
| RinggitSense (Python/FastAPI) | `RinggitSense/CLAUDE.md` | `.orchestrator/subagents/RINGGITSENSE_INIT.md` |
| Sen2Nal (Python/ML) | `Sen2Nal/CLAUDE.md` | `.orchestrator/subagents/SEN2NAL_INIT.md` |
| Portfolio (Spring Boot) | `Portfolio/CLAUDE.md` | `.orchestrator/subagents/PORTFOLIO_INIT.md` |
| AutoRecruit (SPFx/React) | N/A | `.orchestrator/subagents/AUTORECRUIT_INIT.md` |
