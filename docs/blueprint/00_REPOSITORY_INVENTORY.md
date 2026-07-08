# BAiTI — Repository Inventory & Discovery Report

> **Blueprint document 0 of 9** | Produced: 2026-07-07
> **Project**: BAiTI (formerly MyComunity) — multi-tenant SaaS condominium/community management
> **Status of project**: PRE-DEVELOPMENT. Zero code. No git repo. This document inventories every artifact that exists today, rules on contradictions between them, and declares the canonical sources the rest of the blueprint is built on.

---

## 1. Complete Artifact Inventory

### 1.1 Root-level artifacts

| Artifact | Type | Date | Size | Verdict |
|---|---|---|---|---|
| `MyComunity 1.0 data model.pdf` | ERD diagram, 23 tables with column lists | 2026-03-22 | 166 KB | **Source schema.** Authoritative for table/column *inventory*; NOT authoritative for spelling or normalization (see §3). |
| `MyComunity_PLAN.txt` | 527-line SDLC plan (stack, tenancy, migrations, CI/CD, risks) | 2026-04-08 | 25 KB | **Primary planning source.** Still sound. Superseded on naming (MyComunity → BAiTI) and corrected on three defects (§3). |

### 1.2 `meeting/` — Anep alignment materials (2026-04-08)

| Artifact | Purpose | Verdict |
|---|---|---|
| `01_MEETING_BRIEF.md` | Agenda + decisions needed from Anep (vision, MVP split, stack, collaboration model, timeline) | Historical prep doc. **Meeting outcome was never recorded** — this remains the single biggest open item (§5). |
| `02_STACK_RATIONALE.md` | Why Spring Boot / PostgreSQL / React / multi-tenant / monorepo | Current. Adopted into this blueprint's architecture rationale. |
| `03_SPRINGBOOT_REFRESHER.md` | Laravel→Spring Boot translation cheat sheet with code patterns | Reference doc, valid. Its code idioms (entity/repo/service/controller/DTO shapes) are adopted as the project's canonical backend idiom. |
| `04_REACT_REFRESHER.md` | React 18 + TS + Vite stack patterns | Reference doc, valid. Its idioms (TanStack Query, Zustand, RHF+Zod, Axios interceptors) are adopted as the canonical frontend idiom. One security caveat adopted with modification: token storage (see 03_ARCHITECTURE §7.4). |

### 1.3 `docs/ai-context/` — AI memory archive (2026-05-10)

| Artifact | Verdict |
|---|---|
| `README.md` | Archive protocol: status labels, source-of-truth hierarchy, update rules. **Adopted** — this blueprint session complies with its update rules. |
| `00_PROJECT_SOURCE_OF_TRUTH.md` | Stable facts: stack, naming conventions, roles, architecture non-negotiables, migration sequence. Current except: project name (now BAiTI), table-count arithmetic (§3.1), migration ordering defect (§3.3). |
| `01_CURRENT_STATUS.md` | Status as of 2026-05-10: zero code, Phase 0 unverified. Still accurate as of 2026-07-07; refreshed by this session (see `docs/ai-context/01_CURRENT_STATUS.md`). |
| `03_BUG_HISTORY/` (3 files) | Empty templates + pre-implementation risk watchlist (R6, R11, R12, R13). No bugs possible yet. |
| `04_DATABASE_STATE/` (3 files) | Migration table (V1–V29 PLANNED), schema assumptions (base-entity columns, RLS pattern, composite uniques), seed strategy (V27 template roles, V29 two synthetic tenants, PDPA rules). All adopted and extended in 04_DATABASE_DESIGN. |
| `06_TESTING_EVIDENCE/` (3 files) | Empty templates + planned browser-test flows for Phases 5 and 8. Flow definitions adopted into 06_TESTING_STRATEGY (UAT section). |
| `08_DOC_ALIGNMENT/` (2 files) | Docs-vs-code matrix (everything DOC AHEAD OF CODE, as expected pre-code) + doc inventory. Notes a stale `.orchestrator/sessions/MYCOMUNITY_SESSION_2026-04-01.md` upstream (outside this folder) — superseded, ignore. |

### 1.4 `docs/audits/` — most recent authority (2026-05-19)

| Artifact | Verdict |
|---|---|
| `PROJECT_STATE_AUDIT_2026-05-19.md` | 8-role delivery audit. Classification: **A (new project) / H (AI-assisted) / hidden risk I (session continuity)**. Its documentation-gap analysis (§4 of that doc) is the requirements list this blueprint fulfils: SRS, user stories, acceptance criteria, service contracts, API surface, DDL/FK/index map, access-control matrix, threat model, test pyramid, UAT plan, deployment/rollback guides, env-var inventory. |
| `BAITI_LAUNCH_PROMPT.md` | **Newest instruction document (2026-05-19). Establishes the rename MyComunity → BAiTI** with cheat sheet: folder `baiti`, repo `baiti`, package root `my.baiti`, app class `BaitiApplication`, subdomain `condoA.baiti.app` (TBD). Also defines the bootstrap sequence (env verify → CLAUDE.md → naming ADR → repo bootstrap → backend skeleton → V1/V2 → RLS proof) which this blueprint's roadmap and packets incorporate as Milestones M0–M2. |

### 1.5 Tooling residue

| Artifact | Verdict |
|---|---|
| `.memsearch/memory/2026-05-10.md`, `2026-06-02.md` | 19-byte session stubs (`## Session HH:MM`). No content. Ignore. |

### 1.6 What does NOT exist (confirmed by direct inspection)

No `.git/`, no `src/`, no `pom.xml`, no `package.json`, no Dockerfile/docker-compose, no `.gitignore`, no `.env.example`, no `README.md`, no project-root `CLAUDE.md`, no Flyway migration, no test, no CI workflow. **Zero lines of application code.**

---

## 2. Canonical Source Ruling

Where documents overlap or conflict, the rest of this blueprint uses this precedence:

1. **`docs/audits/BAITI_LAUNCH_PROMPT.md`** (2026-05-19) — naming (BAiTI), bootstrap sequencing, hard rules
2. **`MyComunity_PLAN.txt`** (2026-04-08) — stack, tenancy architecture, CI/CD, risks *(as corrected by §3 below)*
3. **`MyComunity 1.0 data model.pdf`** — table/column inventory only, never spelling
4. `docs/ai-context/00_PROJECT_SOURCE_OF_TRUTH.md` + `04_DATABASE_STATE/` — conventions, base-entity pattern, seed rules
5. `docs/audits/PROJECT_STATE_AUDIT_2026-05-19.md` — gap list, risk register
6. `meeting/` docs — rationale and idiom reference

**This blueprint (`docs/blueprint/`) now supersedes items 1–6 as the implementation reference.** The legacy documents remain for traceability; any conflict between them and `docs/blueprint/` resolves in favour of the blueprint.

---

## 3. Contradictions & Defects Found During Discovery

These were found by cross-checking the PDF against every planning document. They are **corrected in this blueprint** and must not be re-introduced by implementation agents.

### 3.1 The "22 tables" count is wrong — the PDF contains 23

Every planning doc says "22-table schema". Direct count of the PDF: **23 tables** (`block`, `units`, `residents`, `family_members`, `users`, `roles`, `permissions`, `roles_permissions`, `bilings`, `payments`, `payment_methods`, `complaint_categories`, `complaints`, `complaints_updates`, `maintenance_requests`, `facilities`, `facility_bookings`, `announcements`, `events`, `visitors`, `visitor_pre_registrations`, `notification`, `activity_logs`).

Final schema = 23 PDF tables + `tenants` + `refresh_tokens` = **25 tables** (26 if the recommended `super_admins` table is confirmed). See 04_DATABASE_DESIGN.

### 3.2 The PDF contains more typos than the plan's correction list

Plan already corrects: `tittle`→`title` (announcements, events), `loacation`→`location`, `messaage`→`message`, `billings`→`bills`, `complaints_updates`→`complaint_updates`, `resident_type(Owner/Type)`→`OWNER|TENANT`.

**Newly found in this discovery pass — additional mandatory corrections:**

| PDF (verbatim) | Corrected | Where |
|---|---|---|
| `premission_id` | `permission_id` | `roles_permissions` pivot |
| `submitted at` (space) | `submitted_at` | `complaints` |
| `update_at` | `updated_at` | `complaints_updates` |
| `block` (singular table name) | `blocks` | property domain |
| `notification` (singular table name) | `notifications` | system domain |
| `Floor_no` (capitalised) | `floor_no` | `units` |
| `bilings` (PDF actually spells it with one *l*, not `billings` as the plan says) | `bills` | finance domain |
| `Id` / `Name` / `Description` (capitalised columns) | `id` / `name` / `description` | throughout |

### 3.3 Migration-sequence defects in the plan

1. **Internal numbering conflict**: Plan §5.4 says V25=enable_rls, V26=seed_roles, V27=seed_dev, V28=refresh_tokens. Plan §8 / Source-of-Truth / migration_status say V26=enable_rls, V27=seed_roles, V28=refresh_tokens, V29=seed_dev. The §8 numbering was the intended one — but both share defect 2:
2. **`refresh_tokens` is created AFTER `enable_rls`** — a blanket RLS migration at V26 cannot apply a policy to a table that doesn't exist until V28. Fixed sequence in 04_DATABASE_DESIGN §6: all tables first, RLS enablement after the last table, seeds last.
3. **RLS owner-bypass not addressed anywhere**: PostgreSQL RLS does **not** apply to the table owner or superusers unless `FORCE ROW LEVEL SECURITY` is set. Flyway (owner) and the application must connect as **different database roles**, and every RLS'd table needs `FORCE`. No planning document mentions this; without it the entire three-layer isolation story is void. Fixed in 03_ARCHITECTURE §6 and 04_DATABASE_DESIGN §7.
4. **`SET LOCAL` semantics**: `SET LOCAL` is a no-op outside a transaction. The plan's "Hibernate interceptor sets PG session var" needs a concrete, pool-safe mechanism. Specified in 03_ARCHITECTURE §6.3.

### 3.4 Normalization gap in the PDF

`payments.payment_method` is a free-text column even though a `payment_methods` table exists. Corrected to `payments.payment_method_id UUID FK → payment_methods(id)` (04_DATABASE_DESIGN §4, finance domain).

### 3.5 Naming

The launch prompt (newest doc) renames the project **BAiTI**. Every new artifact — including all nine blueprint documents — uses BAiTI naming: folder `baiti`, repo `baiti`, package `my.baiti`, class `BaitiApplication`. Legacy filenames (`MyComunity_PLAN.txt`, the PDF) are retained untouched for traceability.

---

## 4. Blueprint Package Map (what this discovery feeds)

| Blueprint doc | Fulfils | Primary inputs |
|---|---|---|
| `00_REPOSITORY_INVENTORY.md` | Phase 1 — Discovery | This document |
| `01_SRS.md` | Phase 2 — Requirements | Plan §1–2, meeting brief, PDF, audit gap list |
| `02_SYSTEM_ANALYSIS.md` | Phase 3 — Analysis | Plan §2/§6, PDF relationships, meeting brief MVP split |
| `03_ARCHITECTURE.md` | Phase 4 — Architecture | Plan §5–7, stack rationale, refreshers, §3 corrections |
| `04_DATABASE_DESIGN.md` | Phase 5 — Database | PDF (columns), plan §5.4, schema_assumptions, §3 corrections |
| `05_QUALITY_ATTRIBUTES.md` | Phase 6 — Quality | Plan §3/§9, audit §6, root SOP §4.4/§8 |
| `06_TESTING_STRATEGY.md` | Phase 7 — Testing | Plan §5.7, testing-evidence templates, audit §7 |
| `07_IMPLEMENTATION_ROADMAP.md` | Phase 8 — Roadmap | Plan §8, launch prompt phases, meeting brief MVP split |
| `08_ai_packets/` | Phase 9 — AI packets | All of the above |
| `README.md` (blueprint index) | Phase 10 — Final package | All of the above |

---

## 5. Open Items Carried Into the Blueprint (decision register)

| # | Item | Owner | Blocks | Blueprint treatment |
|---|---|---|---|---|
| D1 | Anep meeting outcome (stack sign-off, frontend ownership) | Faisal ↔ Anep | ~~Frontend implementation~~ | **RESOLVED 2026-07-07** — user confirmed all points; recorded in `meeting/05_OUTCOME.md`; G-ANEP cleared |
| D2 | GitHub repo location + visibility | Faisal ↔ Anep | First `git push` only | **RESOLVED 2026-07-07** — `github.com/FaisalHanafi98/baiti`, private; push itself still needs explicit instruction (CON-4) |
| D3 | Deployment target | Faisal | Milestone M6 only | **RESOLVED 2026-07-07** — Fly.io, Singapore region (executed at P18) |
| D4 | Billing currency (MYR-only vs multi-currency) | Anep (domain) | Finance module | **Blueprint decision: MYR-only, `amount NUMERIC(12,2)` + ISO currency column defaulted `'MYR'`** — reversible, no schema migration needed later |
| D5 | Language support (EN vs EN+BM) | Anep (domain) | UI copy only | **Blueprint decision: EN-only strings, but i18n-ready from day 1** (react-i18next scaffold, no hard-coded strings) |
| D6 | Super-admin model | Faisal | Tenancy module | **Blueprint decision: separate `super_admins` table** (04_DATABASE_DESIGN §4.1) — recommended by plan, now locked with rationale |
| D7 | Mobile timeline | Both | Nothing (post-v1.0) | Out of scope for v1.0; React chosen to keep the door open |

---

*Discovery performed 2026-07-07 by direct inspection of every file in the repository, including a full read of the data-model PDF. No repository content was modified other than the addition of `docs/blueprint/` and the ai-context refresh mandated by the archive protocol.*
