# BAiTI — Launch Board Prompt

> **Purpose**: Copy-paste prompt for Claude Opus / Claude Code when ready to move BAiTI from planning into repo bootstrap.
> **Renamed from**: MyComunity → **BAiTI**
> **Use when**: Phase 0 (env) and Phase 1 (repo bootstrap) are about to start.

---

## The Prompt

```text
You are acting as a software delivery launch board for my BAiTI project.

Roles:
- Principal Software Architect
- Technical Product Manager
- Senior Spring Boot Engineer
- Senior React Engineer
- Database Architect
- QA Lead
- DevOps Engineer
- Security Reviewer
- AI Workflow Auditor
- Documentation Lead

Context:
BAiTI is a planned multi-tenant SaaS condominium/community management system.

According to the latest audit:
- Overall state: NOT STARTED
- Primary lifecycle category: A. New project from scratch
- Secondary lifecycle category: H. AI-assisted development project
- Hidden risk: I. session continuity / planning docs may be stale
- Planning is strong, but implementation is zero-code
- No git repo
- No backend scaffold
- No frontend scaffold
- No DB migrations
- No tests
- No CI/CD
- No root CLAUDE.md
- Dev environment is unverified
- Folder is still named `Anep's Project`
- Anep meeting outcome / frontend ownership is unconfirmed

Planned stack:
- Spring Boot 3.x
- Java 17
- Maven
- PostgreSQL 16 with Row-Level Security
- Flyway
- Spring Security + JWT
- React 18 + TypeScript + Vite
- Tailwind
- TanStack Query
- Zustand
- React Hook Form + Zod
- JUnit 5
- Testcontainers
- Vitest
- Playwright
- GitHub Actions
- Docker Compose

Planned system:
Multi-tenant SaaS community management platform with modules:
- Tenancy
- Property
- People/RBAC
- Finance
- Complaints
- Maintenance
- Facilities
- Community
- Visitors
- System notifications/logs

Your mission:
Do NOT start feature implementation yet.
Do NOT create controllers for all modules.
Do NOT generate a huge codebase.
Do NOT skip Phase 0.
Do NOT trust old planning docs without refreshing current status.

Your goal is to move BAiTI from:
  PLANNING-COMPLETE / ZERO-CODE
to:
  VERIFIED DEVELOPMENT BASELINE + SAFE REPO BOOTSTRAP

================================================================
PHASE 0 — Planning Reality Refresh
================================================================

1. Read:
   - MyComunity_PLAN.txt   (legacy filename, content still applies to BAiTI)
   - docs/ai-context/00_PROJECT_SOURCE_OF_TRUTH.md
   - docs/ai-context/01_CURRENT_STATUS.md
   - docs/ai-context/README.md
   - meeting/01_MEETING_BRIEF.md
   - meeting/02_STACK_RATIONALE.md
   - meeting/03_SPRINGBOOT_REFRESHER.md
   - meeting/04_REACT_REFRESHER.md
   - docs/ai-context/04_DATABASE_STATE/
   - docs/ai-context/06_TESTING_EVIDENCE/
   - docs/ai-context/08_DOC_ALIGNMENT/

2. Produce a short planning refresh report:

   docs/audits/CURRENT_SYSTEM_STATE.md

   It must answer:
   - Is the project still greenfield?
   - Does the planned stack still stand?
   - What assumptions are stale?
   - What decision is still missing from Anep?
   - What must be confirmed before frontend work starts?
   - What must be done before git init?
   - Confirm the project name change MyComunity → BAiTI everywhere
     (folder, repo, package root, JWT issuer, doc headers, etc.)

3. Create or update:
   - docs/ai-context/01_CURRENT_STATUS.md
   - meeting/05_OUTCOME_PENDING.md

   If Anep's meeting outcome is unknown, mark it explicitly as:
   "UNCONFIRMED — do not begin frontend implementation until resolved."

================================================================
PHASE 1 — Project-Level AI Rules
================================================================

Create a root-level CLAUDE.md.

This file must be concise and action-oriented. It must include:
- project purpose
- project name: BAiTI (formerly MyComunity)
- current state: zero-code / pre-bootstrap
- planned stack
- source-of-truth hierarchy
- non-negotiables:
  * multi-tenant isolation is mandatory
  * PostgreSQL RLS must be tested before feature work expands
  * no secrets committed
  * no direct main commits after repo setup
  * TDD required for tenancy/security
  * every claim needs command output
- allowed commands
- forbidden actions
- documentation update rules
- evidence log rules
- session handoff rules

Also create:
- AGENTS.md
- docs/ai-context/SESSION_RESUME.md
- docs/ai-context/PRE_COMPRESSION_CHECKPOINT.md
- docs/ai-context/POST_COMPRESSION_RESUME_PROMPT.md

================================================================
PHASE 2 — Environment Verification
================================================================

Before creating code, verify the local development environment.

Run on Windows PowerShell:

   java -version
   mvn -v
   node -v
   npm -v
   docker ps
   gh auth status
   git --version

Capture exact output into:

   docs/audits/PHASE0_VERIFICATION.md

If any command fails:
- stop repo bootstrap
- document the missing tool
- suggest install command using winget or official installer
- do not continue until the missing tool is fixed

Expected baseline:
- Java 17+
- Maven available
- Node 18+ or 20+
- Docker running
- Git installed
- GitHub CLI authenticated

================================================================
PHASE 3 — Naming and Repository Decision
================================================================

Before git init:

1. Confirm rename from `Anep's Project` to `baiti` (lowercase folder).
2. Decide GitHub repo name: `baiti` (recommended) or alternative.
3. Decide whether repo is personal or shared.
4. Confirm Java package root: `my.baiti` (replacing `my.comunity`).
5. Create:

   docs/adr/0001-project-repository-and-naming.md

   The ADR must include:
   - history (MyComunity → BAiTI rename)
   - current folder issue
   - chosen folder name
   - chosen repo name
   - chosen Java package root
   - reason
   - consequences (search/replace scope across existing planning docs)

================================================================
PHASE 4 — Repo Bootstrap Only
================================================================

Only proceed if Phase 2 passes.

Create the minimum repo structure:

   baiti/
     CLAUDE.md
     AGENTS.md
     README.md
     .gitignore
     .env.example
     backend/
     frontend/
     infra/
     docs/
       adr/
       audits/
       ai-context/
       architecture/
       database/
       testing/
       security/
       deployment/

Initialize git:

   git init
   git checkout -b main
   git add .
   git commit -m "chore: bootstrap BAiTI repository"

Do not push unless explicitly approved.

================================================================
PHASE 5 — Backend Skeleton Only
================================================================

Create a minimal Spring Boot backend scaffold.

Requirements:
- Java 17
- Maven
- Spring Boot 3.x
- Package root: my.baiti
- include dependencies:
  * Spring Web
  * Spring Data JPA
  * Spring Security
  * PostgreSQL driver
  * Flyway
  * Validation
  * Actuator
  * Testcontainers
  * JUnit 5

Create:
- backend/pom.xml
- main application class (my.baiti.BaitiApplication)
- application.yml
- application-dev.yml
- application-test.yml
- health endpoint via actuator
- one context-load test

Do NOT implement business modules yet.

Run:

   cd backend
   mvn test

Capture output into:

   docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md

================================================================
PHASE 6 — First Database Migration
================================================================

Create only the foundational migrations:
- PostgreSQL extensions, if needed
- tenants table
- minimum audit columns
- no full 23-table schema yet unless explicitly instructed

Create:

   backend/src/main/resources/db/migration/V1__extensions.sql
   backend/src/main/resources/db/migration/V2__create_tenants.sql

Create a Testcontainers test proving:
- database starts
- migration applies
- tenants table exists

Run:

   mvn test

Capture evidence.

================================================================
PHASE 7 — Tenancy / RLS Proof Before Feature Expansion
================================================================

Before implementing modules like finance, complaints, visitors, or
facilities, create a test plan for tenant isolation.

Create:
- docs/security/RLS_TEST_STRATEGY.md
- docs/testing/TENANCY_TEST_PLAN.md

Minimum proof required before feature expansion:
- tenant A cannot read tenant B data
- tenant context is required
- missing tenant context fails closed
- JWT tenant claim must map to database session variable
  or equivalent safe mechanism
- RLS cannot be bypassed by normal application queries

Do not implement all of this yet unless instructed.
Just define the test-first plan and minimal next step.

================================================================
PHASE 8 — Final Output
================================================================

At the end, produce:

1. Current state after your work
2. What was created
3. What was verified
4. Exact command outputs captured
5. What is still unverified
6. Whether the project can now proceed to:
   - backend scaffolding
   - tenancy implementation
   - frontend scaffolding
   - module development
7. Top 5 risks
8. Next recommended prompt for the following session

================================================================
Hard rules
================================================================

- Do not implement full modules.
- Do not skip environment verification.
- Do not create fake test evidence.
- Do not push to GitHub without explicit approval.
- Do not begin frontend implementation until Anep / frontend
  ownership is confirmed.
- Do not implement finance / complaints / facilities before
  tenancy and RLS are proven.
- Prefer one verified scaffold over a large unverified generated
  codebase.
- Treat every occurrence of "MyComunity" in legacy docs as a
  rename target → BAiTI. Do not silently keep the old name in
  any new file.
```

---

## Rename Cheat Sheet (MyComunity → BAiTI)

| Old | New |
|---|---|
| Project name | MyComunity | BAiTI |
| Folder | `Anep's Project` / `mycomunity` | `baiti` |
| Java package root | `my.comunity` | `my.baiti` |
| GitHub repo | `mycomunity` | `baiti` |
| Application class | `MyComunityApplication` | `BaitiApplication` |
| Production subdomain pattern | `condoA.mycomunity.app` | `condoA.baiti.app` (TBD) |
| Doc headers | "MyComunity — …" | "BAiTI — …" |
| Plan file | `MyComunity_PLAN.txt` (keep as legacy, reference only) | New canonical plan: `docs/plans/BAITI_PLAN.md` |

> Note: `MyComunity_PLAN.txt`, `MyComunity 1.0 data model.pdf`, and the existing `docs/ai-context/` files keep their filenames for traceability — but any NEW file created from Phase 1 onward uses BAiTI naming. The planning-refresh report in Phase 0 should record this rename as a formal decision.
