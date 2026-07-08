# BAiTI — AI Implementation Packets · Protocol & Index

> **Blueprint document 8 of 9** | Produced: 2026-07-07
> A packet is a self-contained work order executable by any implementation AI (Claude, GPT, GLM, DeepSeek, Kimi, …). Execute packets **in dependency order**; never start a packet whose dependencies lack evidenced Definition of Done.

---

## 1. Executor Protocol (read before any packet)

1. **Read first, in order**: this file → your packet → the blueprint sections your packet cites. On conflict: blueprint (`docs/blueprint/`) beats every older doc in this repo (`00_REPOSITORY_INVENTORY.md §2`); your packet beats nothing — if the packet contradicts the blueprint, stop and log the deviation (rule 8).
2. **TDD is mandatory**: write the failing test named after the acceptance criterion, watch it fail, implement, watch it pass. No production code without a failing test first.
3. **Naming**: project BAiTI · package root `my.baiti` · app class `BaitiApplication`. Any occurrence of "MyComunity/my.comunity" in new code is a defect.
4. **Schema**: `04_DATABASE_DESIGN.md` is the only DDL source. Never generate columns from the PDF. The deviation register (04 §9) is normative.
5. **Scope discipline**: implement exactly the packet. No opportunistic refactors, no extra endpoints, no "while I'm here". Missing prerequisites = a blocked packet, not an improvised fix.
6. **Commit protocol** (CON-4): stage specific files → draft `type(scope): message` → present to user → user edits/approves → commit. **Never push. Never auto-commit.**
7. **Evidence**: DoD claims require captured command output (test run, curl, screenshot) in the PR/session notes; milestones additionally file to `docs/ai-context/06_TESTING_EVIDENCE/` (06_TESTING §10).
8. **Session end**: update `docs/ai-context/01_CURRENT_STATUS.md` + add a `docs/ai-context/02_SESSION_HISTORY/` entry (archive README rules). Log any deviation you had to make, labelled per the archive's status vocabulary (e.g. `ACCEPTED DEVIATION`).
9. **Guard rails**: never edit applied Flyway migrations · never hand-edit `frontend/src/api/types.gen.ts` · never commit secrets/`.env` · never weaken an I-test or authz-matrix row to make a build pass.

## 2. Packet Index

| Packet | Title | Size | Depends on | Gate |
|---|---|---|---|---|
| P00 | Environment verification | S | — | — |
| P01 | Repo bootstrap & governance | S | P00 | G-NAME |
| P02 | Backend skeleton | M | P01 | — |
| P03 | Database migrations V1–V28 | L | P02 | — |
| P04 | JPA entities & repositories | L | P03 | — |
| P05 | Security & tenancy core (5 slices) | XL | P04 | **G-RLS on exit** |
| P06 | CI/CD foundation | M | P05 | G-PUSH for remote |
| P07 | Frontend shell | L | P05 | **G-ANEP on entry** |
| P08 | Property module | M | P05 (BE) / P07 (FE) | — |
| P09 | People module | L | P08 | — |
| P10 | Finance module | L | P09 | — |
| P11 | Complaints module | L | P09 | — |
| P12 | Maintenance module | M | P09 | — |
| P13 | Dashboards & observability | M | P10–P12 | — |
| P18 | Deployment & go-live | L | P13 | D3 decided |
| P14 | Facilities module (v1.x) | L | P18 | — |
| P15 | Community module (v1.x) | M | P18 | — |
| P16 | Visitors module (v1.x) | L | P18 | — |
| P17 | Notification centre & jobs (v1.x) | M | P18 | — |

## 3. Common Pattern for Module Packets (P08–P12, P14–P17)

Module packets specify only their deltas; everything here applies to all of them.

**Backend slice (always first):**
1. DTO records (`Create…Request`, `…Response`, `Update…Request`) with Bean Validation, per idiom `meeting/03_SPRINGBOOT_REFRESHER.md §3.5`.
2. Service with business rules + state-machine guards (02_SYSTEM_ANALYSIS §4); `@Transactional` on writes; own-scope predicate for `*_OWN` permissions; invalid transition ⇒ 409; foreign/missing resource ⇒ 404.
3. Controller under `/api/<resource>`; `@PreAuthorize` authority per the access matrix (02 §8); pagination per 03_ARCHITECTURE §4.
4. Domain events for side-effects listed in the packet (audit always; notifications where 02 §4 says so).
5. Tests: slice tests (real PG) covering happy path, each validation failure class, each state transition (legal + illegal), authz matrix rows for every new endpoint, own-scope proof, **and one cross-tenant I-2-style probe for the new resource**.

**Frontend slice (after contract freeze):**
1. Regenerate `types.gen.ts` from the updated OpenAPI artifact.
2. TanStack Query hooks (`useX`, `useCreateX`…) — no `useEffect` fetching; mutations invalidate the right keys.
3. Pages/components per journey (02 §5) with RHF+Zod forms mirroring backend validation; `<Can>` gates actions; i18n keys only; WCAG checklist (05 §7).
4. Vitest: form validation behaviour, permission gating, one MSW-backed happy path per screen.

**Module DoD (all of):** backend + frontend tests green · coverage gates hold · isolation suite still green · OpenAPI diff non-breaking (or version note) · journey step demo evidence · access-matrix fixture updated · `docs/ai-context/` updated · user-approved commits.
