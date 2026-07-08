# BAiTI — Engineering Handover Package

> **Date**: 08 July 2026 · **Reason**: session context limit; execution transfers to another model
> **Audience**: the next AI model / engineer, assumed to have ZERO conversation history but full repository access.
> Everything referenced here is a committed file in this repository. Where this handover and a repository document overlap, the repository document is the authority; this handover tells you where to look and what state things are in.

---

## Section 1 — Project Overview

**BAiTI** (formerly MyComunity) is a multi-tenant SaaS condominium/community management platform: one deployment serves N communities ("tenants": condos), with tenant data isolated at three independent layers (JWT claim → Hibernate `@TenantId` filter → PostgreSQL row-level security with a forced, non-owner runtime role). Modules: tenancy, auth/RBAC, property (blocks/units), residents/family, finance (bills/payments), complaints, maintenance, facilities, community, visitors, notifications, audit.

**Stack (locked)**: Spring Boot 3.x / Java 17+ (machine has Temurin 21) / Maven · PostgreSQL 16 + RLS · Flyway · React 18 + TypeScript + Vite + Tailwind · TanStack Query + Zustand · JUnit 5 + Testcontainers · Vitest · GitHub Actions · Docker Compose.

**Engineering objective**: portfolio-grade enterprise discipline — TDD everywhere, isolation proven by a permanent CI test suite before any feature work, evidence-before-claims, structured docs.

**The whole engineering programme is written down**: `docs/blueprint/README.md` is the master index of 9 blueprint documents plus 19 executable work packets (`docs/blueprint/08_ai_packets/`). This blueprint was produced 2026-07-07 from a full audit of all prior planning material and **supersedes** the legacy plan (`MyComunity_PLAN.txt`) and the data-model PDF wherever they conflict — the corrections and the reasons are in `docs/blueprint/00_REPOSITORY_INVENTORY.md §3` (notably: an RLS owner-bypass defect, a migration-ordering defect, and eleven schema deviations registered in `04_DATABASE_DESIGN.md §9`). The 2026-05-19 audit that motivated all this is `docs/audits/PROJECT_STATE_AUDIT_2026-05-19.md`.

## Section 2 — Current Execution Status

| Level | Position |
|---|---|
| Epic | **E1 Foundation** (of E1–E7, roadmap `07_IMPLEMENTATION_ROADMAP.md §1`) |
| Milestone | Inside **M1** (M0 done) |
| Packet (module) | **P01 repo bootstrap — COMPLETE** at commit `467d574` (root commit, branch `main`) |
| Next packet | **P02 backend skeleton — NOT STARTED** (deliberately; session-pause instruction) |
| Overall v1.0 progress | Planning 100 % · implementation ~2 of 19 packets (P00, P01) |

Per-activity status for the completed span: planning ✓ · implementation ✓ (docs/infra only — zero application code exists yet, by design) · integration N/A · testing = environment verification only · documentation ✓.

## Section 3 — Completed Work

**Blueprint programme (2026-07-07, this session)** — produced the entire `docs/blueprint/` tree: `00` inventory+contradiction rulings, `01` SRS (FR/NFR IDs are stable and referenced everywhere), `02` system analysis (state machines, journeys J1–J5, role×permission matrix §8), `03` architecture (normative tenancy §6 and auth §7), `04` database design (26 relations, migrations V1–V28, deviation register §9), `05` quality attributes (STRIDE model §3), `06` testing strategy (isolation suite I-1…I-9 §5 = release gate G-RLS), `07` roadmap (milestones/gates/risks), `08` packets P00–P18. Validation: internal cross-referencing; no code to test. Accepted by user ("yes to all of the questions", 2026-07-08).

**P00 environment (2026-07-08)** — evidence in `docs/audits/PHASE0_VERIFICATION.md`. Java 21 Temurin, Node 24, git 2.54, gh authenticated (`FaisalHanafi98`). **Maven 3.9.16 installed manually** to `C:\Users\asbou\tools\apache-maven-3.9.16` (user PATH updated) because `winget install Apache.Maven` does not exist — the legacy plan was wrong about that. **Docker deferred**: Docker Desktop 4.78 is installed but the daemon would not start and launch attempts spawned no process; user said to skip it for now.

**P01 bootstrap (2026-07-08, commit `467d574`, 72 files)** — `.gitignore`/`.gitattributes` (LF forced on `*.sh` — the db-init script would otherwise break in the Linux container), `.env.example` (full env-var contract per architecture §8), `README.md`, `CLAUDE.md` + `AGENTS.md` (project agent rules), `.gitmessage` (commit template), `infra/docker-compose.yml` + `infra/db-init/01_roles.sh` (creates `baiti_migrator` owner + `baiti_app` NOBYPASSRLS — the two-role split RLS depends on), ADR-0001 (naming) and ADR-0002 (monolith/monorepo), full `docs/` tree, and decision records. Known gap: the compose stack is **authored but unverified** (Docker). No regressions possible (no code).

**Decisions cleared (user, 2026-07-08)** — G-ANEP cleared (`meeting/05_OUTCOME.md`); D2 = `github.com/FaisalHanafi98/baiti`, private (repo NOT yet created — pushing requires a fresh explicit instruction); D3 = Fly.io Singapore; G-NAME approved (rename pending, see Section 7); commit-message SOP adopted from CREAMS (see Section 9, rule 6).

## Section 4 — Current Module State

P01 is complete and committed; see `CURRENT_MODULE_STATE.md` (repo root) for the live snapshot. Two items remain inside P01's scope, both deliberately deferred and recorded:

1. **Compose verification** — `docker compose -f infra/docker-compose.yml up -d`, then confirm both roles: `psql -U baiti_app -d baiti -c "select current_user"` and `SELECT rolbypassrls FROM pg_roles WHERE rolname='baiti_app'` (must be `f`). Blocked by the Docker daemon.
2. **Folder rename** — `Anep's Project (Baiti)` → `baiti`; approved but the directory was locked by the running session. Command (run when no session holds the folder): `Rename-Item "C:\Users\asbou\Desktop\Work\Development\Anep's Project (Baiti)" -NewName "baiti"`. Git is unaffected by the parent-folder name.

No partial refactors, no incomplete files, working tree clean.

## Section 5 — Next Execution Steps

**Finish P01 residue** (≤ 15 min once Docker cooperates): start Docker Desktop (if it hangs: `wsl --status`, consider the 4.78→4.81 update winget offers) → `docker ps` → compose up → the two role checks above → append outputs to `docs/audits/PHASE0_VERIFICATION.md`.

**Then P02 backend skeleton** — the packet is fully specified in `docs/blueprint/08_ai_packets/PACKET_02_backend_skeleton.md`: Maven project under `backend/` (package `my.baiti`, class `BaitiApplication`), dual-datasource config (Hikari as `baiti_app`, Flyway as `baiti_migrator`), profiles, Spotless/JaCoCo/OWASP plugins, one Testcontainers health test. Exit: `./mvnw verify` green, evidence to `docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md`. Integration concerns: none yet (first code packet).

## Section 6 — Validation Status

| Kind | Status |
|---|---|
| Code inspection | N/A — no application code exists |
| Static analysis | N/A |
| Compilation / build | N/A (first build arrives with P02) |
| Unit / integration testing | N/A |
| Manual verification | ✓ toolchain versions (P00 evidence file); ✓ git staging hygiene (no secrets, `.memsearch/` ignored, 72-file set reviewed); ✓ commit landed and tree clean (`git status` empty, `git log` = `467d574`) |
| **Unverified** | docker-compose stack + role bootstrap script (never executed); `.gitmessage` template not yet set in local git config; everything downstream of P02 |

## Section 7 — Known Issues

1. **Docker Desktop 4.78 daemon does not start** (blocker for P02's Testcontainers). Diagnostics untried: WSL2 health, version update, reboot.
2. **Folder rename pending** (session file lock) — command in Section 4.
3. `.mvn` wrapper does not exist yet — P02 creates it; until then builds need the PATH-installed Maven.
4. Legacy docs contain the old name/plan by design (traceability); a reader who starts from `MyComunity_PLAN.txt` instead of the blueprint will resurrect known defects — the guard rails are in `CLAUDE.md` and `00_REPOSITORY_INVENTORY.md §2`.

## Section 8 — Backlog (discovered, intentionally deferred)

| Item | Reason deferred | Priority | Depends on |
|---|---|---|---|
| Update Docker Desktop 4.78 → 4.81 | daemon issue may be fixed by it; user paused Docker work | High (pre-P02) | user machine access |
| `git config commit.template .gitmessage` locally | local config, one-liner | Low | — |
| Per-role session policy for shared Guard devices | v1.0 keeps global refresh TTL (analysis 02 §9) | Low | P16 |
| `family_members.age` → `date_of_birth` | PDF fidelity kept for v1.0 (04 §4.4 caveat) | Low | v1.x schema change |
| Facility slots normalisation (table instead of label) | v1.0 uses slot labels (04 §4.7) | Low | P14 usage data |
| Photo attachments on complaints | StorageService lands in P10; DTO kept open (P11 note) | Medium | P10, P11 |
| Blue/green deploys | single-node stop-start accepted at v1.0 (05 §5) | Low | post-v1.0 |

## Section 9 — Next Model Instructions

You are continuing BAiTI, not restarting it. In order:

1. **Read** `CLAUDE.md` (root of this repo) — the binding rules. Then `docs/ai-context/README.md` (resume protocol) → `docs/ai-context/01_CURRENT_STATUS.md` → `CURRENT_MODULE_STATE.md` → `docs/blueprint/08_ai_packets/README.md` (executor protocol).
2. **Current objective**: clear the two P01 leftovers (Section 4), then execute packet P02 exactly as written. Do not begin P03+ in the same breath; each packet ends with evidence and user-approved commits.
3. **Constraints**: TDD (failing test first, always); schema only from `04_DATABASE_DESIGN.md`, never the PDF; the two-DB-role model is untouchable; cross-tenant responses are 404; no secrets in the repo; never edit applied Flyway migrations; BAiTI naming only.
4. **Scope discipline**: implement exactly the packet, nothing opportunistic. If a packet conflicts with reality, stop and record the deviation in the session log; do not improvise architecture.
5. **Quality bars**: `./mvnw verify` green before any done-claim, with output captured; coverage gates 70 %/60 % once CI exists; the isolation suite (from P05 onward) may never be weakened to pass a build.
6. **Git**: stage specific paths; draft the commit message in the CREAMS SOP format (template: `.gitmessage`; reference: `../CREAMS/docs/COMMIT_MESSAGE_SOP.md`) — structured body, real "Verified that" checks, UK English, footer `[Assisted by AI, reviewed manually by Faisal]`, **no Co-Authored-By AI trailers** — present it to Faisal, commit only after his approval. **Never push**; the GitHub repo does not exist yet and creating/pushing needs his explicit instruction.
7. **Session end duties**: update `docs/ai-context/01_CURRENT_STATUS.md`, add a `docs/ai-context/02_SESSION_HISTORY/` entry, refresh `CURRENT_MODULE_STATE.md`, file test evidence when tests ran.
8. **Success criteria for your first session**: Docker verified, compose roles proven, P02 DoD checklist fully evidenced, tree clean, docs updated, commits approved.

## Section 10 — Final Checklist

✓ Current architecture (Section 1 + `docs/blueprint/03`) · ✓ roadmap status (Section 2) · ✓ completed work (Section 3) · ✓ remaining work (Sections 4–5) · ✓ current module state (Section 4 + `CURRENT_MODULE_STATE.md`) · ✓ validation status (Section 6) · ✓ known issues (Section 7) · ✓ deferred improvements (Section 8) · ✓ next execution steps (Section 5) · ✓ next-model instructions (Section 9).
