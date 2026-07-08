# BAiTI — Engineering Blueprint (Master Index)

> **Produced**: 2026-07-07 · **Project state**: pre-development, zero code
> **What this is**: the complete, implementation-ready engineering package for BAiTI (formerly MyComunity) — a multi-tenant SaaS condominium/community management platform. It was produced by a full discovery pass over every artifact in this repository and is designed so that any implementation AI (Claude, GPT, GLM, DeepSeek, Kimi, …) or human engineer can build the system safely and consistently.
> **Authority**: within this repository, `docs/blueprint/` supersedes all older planning documents (`MyComunity_PLAN.txt`, the PDF, `docs/ai-context/` planning facts) wherever they conflict — the ruling and the reasons are in `00_REPOSITORY_INVENTORY.md §2–3`.

---

## The Package

| # | Document | Answers |
|---|---|---|
| 0 | [00_REPOSITORY_INVENTORY.md](00_REPOSITORY_INVENTORY.md) | What exists, what contradicts what, which sources are canonical, which decisions are still open (D1–D7) |
| 1 | [01_SRS.md](01_SRS.md) | Business requirements · functional requirements (FR-…) with MoSCoW priorities · NFRs · constraints · assumptions · unknowns |
| 2 | [02_SYSTEM_ANALYSIS.md](02_SYSTEM_ANALYSIS.md) | Scope/non-goals · module map · use cases · workflow state machines · user journeys J1–J5 · data flows · integrations · **access-control matrix** |
| 3 | [03_ARCHITECTURE.md](03_ARCHITECTURE.md) | Stack + rationale · layering rules · API strategy · **normative multi-tenancy design (three layers, dual DB roles, fail-closed)** · auth · config · logging · caching/async · scalability · deployment matrix · DR · ADR index |
| 4 | [04_DATABASE_DESIGN.md](04_DATABASE_DESIGN.md) | The **normative schema**: 26 relations fully specified · ERD · corrected migration sequence V1–V28 · RLS DDL pattern · integrity rules · **PDF deviation register** |
| 5 | [05_QUALITY_ATTRIBUTES.md](05_QUALITY_ATTRIBUTES.md) | Performance budgets · scalability levers · **STRIDE threat model** · reliability · availability · maintainability · accessibility · i18n · observability · explicit trade-offs |
| 6 | [06_TESTING_STRATEGY.md](06_TESTING_STRATEGY.md) | Test pyramid · Testcontainers backbone · **tenant-isolation suite I-1…I-9 (release gate)** · authz-matrix testing · UAT · performance/stress · security testing · evidence rules |
| 7 | [07_IMPLEMENTATION_ROADMAP.md](07_IMPLEMENTATION_ROADMAP.md) | Epics → milestones M0–M7 · gates (G-NAME, G-RLS, G-ANEP, G-PUSH) · dependencies · consolidated risk register · estimates · rollback rules |
| 8 | [08_ai_packets/](08_ai_packets/README.md) | **19 executable work orders** P00–P18: objective, scope, dependencies, files, architecture, tests, Definition of Done — plus the executor protocol |

## How to Use This Package

**If you are an implementation AI starting work**: read `08_ai_packets/README.md` (the protocol), then execute **P00** (environment verification). Follow the packet order; respect the gates; never start a packet whose dependencies lack evidenced DoD.

**If you are Faisal/Anep reviewing**: read 01 (what we're building) → 02 §5 (the journeys) → 07 (the plan and the four decisions that gate it). The seven open decisions are consolidated in `00 §5` — three of them (D1 Anep outcome, D2 repo location, D3 deploy target) are the only things a human must resolve; everything else has a decided-but-reversible baseline.

**If a session is resuming cold**: follow `docs/ai-context/README.md` (unchanged) — then treat this blueprint as the planning source of truth.

## What Discovery Fixed (the reasons this package isn't just a re-format of the old plan)

1. **The "22-table" count was wrong** — the PDF holds 23 tables; final schema is 26 relations (00 §3.1, 04 §2).
2. **Eight additional PDF typos/name defects** beyond the old correction list, including `premission_id` and singular table names (04 §9 — complete register).
3. **The planned migration order was defective** — `refresh_tokens` was created *after* the RLS migration; internal numbering conflicted between plan sections. Fixed sequence: 04 §6.
4. **RLS would not have worked as planned** — PostgreSQL RLS does not bind table owners; no planning doc separated the Flyway role from the runtime role. Fixed with dual roles + `FORCE ROW LEVEL SECURITY` + a permanent CI canary (03 §6.2, RR-2).
5. **`SET LOCAL` semantics** — the planned "interceptor" was a no-op design outside transactions; replaced with a pool-safe `TenantAwareDataSource` set/reset pattern (03 §6.3).
6. **`payments.payment_method` free-text** normalized to an FK (04 §4.5).
7. **Missing engineering artifacts the 2026-05-19 audit demanded** now exist: SRS, use cases, journeys, access-control matrix, STRIDE threat model, env-var inventory, test pyramid + UAT plan, deployment/rollback/DR plans, and the AI packets.

## Ground Rules Carried Everywhere (non-negotiable)

- Multi-tenant isolation proven before feature work (**G-RLS**); isolation suite green forever.
- TDD on all implementation; evidence before any "done" claim.
- Commit protocol: draft → user reviews/edits → commit; **never push without explicit instruction**.
- No secrets in the repo; synthetic data only (PDPA).
- Applied migrations are immutable; the deviation register (04 §9) is law for schema spelling.
- BAiTI naming in every new artifact (`baiti` / `my.baiti` / `BaitiApplication`).
