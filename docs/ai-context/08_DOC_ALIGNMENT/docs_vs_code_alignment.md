# Docs vs Code Alignment Matrix

## Scoped current review — 4 November 2026

Continuation after owner implementation direction: catalogue/default sync,
method-authority enforcement and actor stamps are now implemented and verified
in [302 native tests](../06_TESTING_EVIDENCE/2026-11-04_permissions.md). The
initial inspection table below remains a historical planning baseline. Own-scope,
onboarding, async activity audits and full G-RLS remain open.

The May matrix below is historical and is superseded for the reviewed foundation
by [current status](../01_CURRENT_STATUS.md) and the
[P05c acceptance map](../../blueprint/08_ai_packets/PACKET_05_security_tenancy.md#planning-baseline-and-acceptance-map--4-november-2026).
This is source inspection at `igumi` / `4ce314b`, not a test run or a complete
domain-alignment audit.

| Topic | Current observation | Classification / action |
|---|---|---|
| Source-of-truth/archive navigation | `00_PROJECT_SOURCE_OF_TRUTH.md` and the AI-context README retain May pre-code/old-path statements; the README names a nonexistent standalone deviation-register file | STALE DOC. Use current status, blueprint and schema §9. Preserve historical content; no broad rewrite in this planning slice. |
| Runtime, migrations and authentication | POM declares Boot 4.1.2/Java source 17; V1–V31 and context/JWT/login/refresh source exist | May "no code" rows are superseded. The 276-test record is dated 3 November; no fresh runtime pass claimed. |
| Permissions and actors | Role/grant entities and JWT permission projection exist; catalogue/sync/method enforcement missing in inspected source; JpaConfig auditor is empty | PARTIAL. Execute the bounded P05c work order before onboarding and full G-RLS. Preserve existing authentication. |
| Default-grant startup wording | Packet describes startup default sync; FR-RBAC-02 seeds on tenant creation and permits Admin edits | CONTRACT RECONCILIATION REQUIRED before startup writes: distinguish initial/new permissions from intentionally revoked grants. |
| Landed-RA and new module proposals | Owner-confirmed RA pilot/readiness plans are in the original dirty checkout; new procurement/search/reserve guides are planning handoffs | Proposed scope does not silently replace the blueprint or complete P08–P12. Accept affected domain/permission/schema contracts first. |
| Branches, frontend and hosting | Local main/igumi/anep refs align; frontend directory absent; Docker/hosting deferred | Keep the three-branch workflow. Live GitHub and hosted CI were not rechecked. |

## Historical matrix — May 2026

> **Last Updated**: 2026-05-10
> **Evidence Basis**: File inspection of `Anep's Project/` folder + `.orchestrator/sessions/`

---

## Alignment Matrix

| Topic / Feature | Existing Docs Say | Codebase Shows | Status | Evidence | Recommended Action |
|---|---|---|---|---|---|
| **Tech stack** | Spring Boot 3 + Java 17 + Maven + PostgreSQL 16 + React 18 + TypeScript + Vite + Tailwind (from plan 2026-04-08) | No code exists | DOC AHEAD OF CODE | `MyComunity_PLAN.txt` line 6 | Begin Phase 0 (env setup) |
| **Stack decision (April 1 session)** | "Stack has NOT been decided yet" — presents Laravel/Next.js/Spring Boot as options | Plan from 2026-04-08 says Spring Boot confirmed | STALE DOC | `MYCOMUNITY_SESSION_2026-04-01.md` vs `MyComunity_PLAN.txt` | April 1 doc is superseded — archive it |
| **Multi-tenancy** | Multi-tenant from day 1, RLS + TenantContext + JWT claims (plan) | No code | DOC AHEAD OF CODE | Plan section 5.5 | Implement per plan |
| **Database schema** | 23 tables (22 PDF + tenants), typos corrected, with `tenant_id` on all domain tables | No database, no migrations | DOC AHEAD OF CODE | Plan section 2.2 + 3 | Write Flyway migrations per plan |
| **Directory name** | Plan says rename to `MyComunity` | Directory still named `Anep's Project` | DEVIATION | Filesystem inspection | Confirm with user if Anep approved rename, then rename |
| **Git repository** | Plan says `git init` in Phase 1 | No git repo | DOC AHEAD OF CODE | Filesystem (no `.git` folder) | Execute Phase 1 |
| **GitHub repo** | Plan says defer until Anep confirms location | No repo exists | DOC AHEAD OF CODE | Plan section 8, Phase 1 step 1.7 | Resolve Anep alignment first |
| **Project CLAUDE.md** | Plan lists `MyComunity/CLAUDE.md` as critical file | Does not exist | DOC AHEAD OF CODE | Plan section 12 critical files | Create in Phase 1 |
| **Flyway migrations** | Plan documents V1–V29 in detail | No migration files | DOC AHEAD OF CODE | Plan section 5.4 | Execute Phase 3 |
| **Spring Boot backend** | Plan documents full package-by-feature structure | No backend code | DOC AHEAD OF CODE | Plan section 5.3 | Execute Phase 2 |
| **React frontend** | Plan documents Vite + TanStack Query + Zustand setup | No frontend code | DOC AHEAD OF CODE | Plan section 5.6 | Execute Phase 6 |
| **CI/CD pipelines** | Plan documents GitHub Actions workflows | No workflow files | DOC AHEAD OF CODE | Plan section 7.3 | Execute Phase 7 |
| **Dev environment** | Plan states Java 8 only, no Maven/Node/Docker (as of April 8) | UNKNOWN — may have changed | UNKNOWN | No terminal evidence since plan was written | Verify by running tool version checks |
| **Anep alignment** | Meeting brief prepared for 2026-04-08 | Outcome unknown | UNKNOWN | No post-meeting record found | Ask user whether meeting occurred |
| **PDF typo corrections** | Plan documents 6 typo corrections | Not yet implemented (no schema) | DOC AHEAD OF CODE | Plan section 2.2 | Apply corrections in Flyway migrations |

---

## Summary

- **All docs-vs-code relationships**: DOC AHEAD OF CODE
- This is expected — the project is in pure planning phase
- No actual code deviations possible because no code exists
- The only true deviation is the directory name and the stale April 1 session document

---

## Note on Stale April 1 Session Document

The `.orchestrator/sessions/MYCOMUNITY_SESSION_2026-04-01.md` was written one week before the SDLC plan was finalized. It contains outdated assumptions:

1. "Stack has NOT been decided yet" — SUPERSEDED by April 8 plan
2. Suggests `laravel new MyComunity` as an option — SUPERSEDED
3. Exit criteria include "Create GitHub repo + push" — plan section 7.1.1 now says NO pushes without explicit user instruction

**Recommended action**: Move this file to `09_ARCHIVE/` or annotate it as superseded. Do not use it as an active session template.
