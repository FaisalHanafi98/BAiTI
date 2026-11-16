# BAiTI session migration prompt

**Checkpoint · 17 November 2026 · Workstation and AI session handoff**

Start here when discovering, researching, migrating or resuming BAiTI.
[AGENTS.md](AGENTS.md) routes new sessions to this file. Use the checkpoint as
a map to the evidence. Current source, verification and owner decisions take
precedence over dated observations.

## Prompt for the new session

> Reconstruct BAiTI from this folder in one pass. Start with
> `SESSION_MIGRATION_PROMPT.md` and follow its discovery order. Verify the
> current branch and the planning materials in the canonical BAiTI checkout. Inspect
> the source, history, dated tests and accepted decisions before reporting
> what is implemented.
>
> Use the available evidence to avoid asking me to repeat previous answers.
> If a file is missing, look in the corresponding Git revision or the documented archive. Record any gaps and continue.
> Return one integrated resume report with the actual baseline, open
> decisions, evidence and next bounded work package. If my current request
> also authorises implementation, proceed once its prerequisites are met.

Discovery starts read-only. Use the current request and applicable project
rules to determine authority for source edits, Git changes, database writes,
external messages and deployment. Keep credentials, resident data and private
raw transcripts out of reports and Git.

## 1. Locate the BAiTI checkout

Open the canonical `baiti` folder. It contains the code, requirements and
local planning records. Faisal develops on `igumi`, Anep uses `anep`, and
`main` is the production branch. Use Git branches in this folder to separate
work. Earlier worktree names in dated records refer to historical snapshots.

Check `git status --short --branch`, `git log -12 --oneline`, and
`git worktree list` before changing branches. Use the
[current status](docs/ai-context/01_CURRENT_STATUS.md) and
[consolidation record](docs/ai-context/02_SESSION_HISTORY/2026-11-17_single_checkout.md)
to distinguish local edits from the published baseline.

## 2. One-pass discovery order

1. **Rules and current state.** Read the parent workspace `AGENTS.md` and
   `standards/README.md` where present. Read the checkout's
   `AGENTS.md`, `CLAUDE.md`, `README.md`,
   `docs/ai-context/00_PROJECT_SOURCE_OF_TRUTH.md`,
   `docs/ai-context/01_CURRENT_STATUS.md` and
   `docs/ai-context/02_SESSION_HISTORY/2026-11-08_p05_native_exit.md`.
   Read later records if the verified branch has advanced. Policy instructions
   still apply when a status paragraph in the same document is older.
2. **Engineering contracts.** Under the implementation root's
   `docs/blueprint/`, read `README.md`, `00_REPOSITORY_INVENTORY.md`,
   `01_SRS.md`, `02_SYSTEM_ANALYSIS.md` including the permission matrix,
   `03_ARCHITECTURE.md` sections 6–7, `04_DATABASE_DESIGN.md` including
   the deviation register, `05_QUALITY_ATTRIBUTES.md`,
   `06_TESTING_STRATEGY.md` sections 3.6 and 5, and
   `07_IMPLEMENTATION_ROADMAP.md`. Then use `08_ai_packets/README.md` and
   the relevant P05 and P08 work orders. The accepted native test contract
   supersedes older Compose prerequisites.
3. **Source and verification.** Trace the classes, migrations, routes and
   tests in sections 4–5. Read `docs/testing/NATIVE_POSTGRES.md`,
   `docs/security/RLS_TEST_STRATEGY.md` and the 8 November test records.
   Distinguish source inspection, local execution, GitHub publication, hosted
   CI, release and pilot adoption.
4. **Product and design.** Read the index in section 3 from the original
   planning folder or a transferred copy. When resolving a disagreement,
   trace the original statement, its reconciliation disposition, the owner's
   decision and the actual source. Preserve uncertainty where approval or
   implementation evidence is missing.
5. **Inventory.** Review the complete relevant BAiTI tree before reporting an
   item missing. Cover the root, `docs/`, `anep/`, `archive/`, `backend/`,
   `tools/`, `infra/` and any transferred planning folder. Use Git file
   lists and targeted searches. Exclude build output, dependency caches,
   PostgreSQL data, raw test logs and Git objects from broad searches. Classify
   material as tracked, modified, untracked, ignored or archival.

If transferred planning files are missing, continue with the published
blueprint, history and evidence. Report the missing inputs once, separately
from unfinished product work.

## 3. Product direction and planning index

- The owner confirmed a Malaysian **landed residents' association (RA)**
  pilot. **Hybrid** means RA management plus neighbourhood commerce. The
  hosting budget is **undecided**. Native PostgreSQL and GitHub are the
  current development route. Docker, Testcontainers, VPS and deployment
  are deferred.
- The consolidated direction starts with committee access, property and
  residency, address verification and suitable announcements. RA finance
  requires explicit liable-party and effective-date rules. Verification,
  membership, debt and committee permissions are separate concepts.
- Anep's initial draft covers trust tiers, monthly invoicing, DuitNow QR/FPX,
  WhatsApp reminders, transparent finances, SOS, QR patrols, porch or
  permitted pickup-point collection, food preorders, local services, neighbour
  vouches, carpool, RA SaaS, transaction fees, paid pins and adverts. Each
  has its own decision and acceptance gates. SOS and patrol need separate
  operating pilots. Carpool and paid promotion sit outside the current build.
- Extend the existing Spring/PostgreSQL monolith. `TAMAN` maps to
  `tenants`. Reuse the identity, property, billing and audit foundations.
  A gateway intent, a confirmed payment, a redacted financial view and a
  preorder reservation have distinct rules. Verify current primary sources
  and actual pilot arrangements before making provider, regulatory, safety,
  retention or pricing claims. Justify proposed changes from evidence.

| Source in the planning folder | What to recover |
|---|---|
| [Documentation index](docs/README.md) and [stakeholder consolidation](docs/plans/STAKEHOLDER_RECONCILIATION_2026-10-22.md) | File map, owner decisions, Codex/Gemini comparisons and staged RA-first work |
| [Research pack](docs/research/stakeholder-review-2026-10-22/README.md), [reconciliation ledger](docs/research/stakeholder-review-2026-10-22/consolidation/RECONCILIATION_LEDGER.md) and [owner decisions](docs/research/stakeholder-review-2026-10-22/consolidation/OWNER_DECISIONS.json) | Original stakeholder draft, Codex research, rendered Gemini report, 60 decisions mapped to 164 review units, and source audit. Use the preserved report if its share link is unavailable. |
| [Anep review](docs/plans/ANEP_ERD_CONSOLIDATION_2026-10-26.md) and [source ERD Markdown](<anep/md file/erDiagram.md>) | Read the ERD alongside the class, flowchart and sequence Markdown in `anep/md file/`. The PDFs and image are exports. The review links proposed capabilities to existing entities. |
| [Consolidated ERDs](docs/database/consolidated-erd/README.md) and [offline viewer](docs/database/consolidated-erd/index.html) | Overview, eight diagrams, editable Mermaid/DOT, visual exports, `MODEL.json` and data dictionary. The October ERDs are logical proposals. For applied schema, use the current SQL and migration history. |
| [Implementation readiness](docs/plans/IMPLEMENTATION_READINESS_2026-10-27.md), [actor register](docs/plans/ACTOR_REGISTER_2026-10-27.md) and [machine-readable registers](docs/plans/READINESS_REGISTERS_2026-10-27.json) | Actors, requirements, acceptance criteria, staged delivery and the native-development decision. The original master prompt is under `docs/research/development-initialization-2026-10-27/`. |
| [Collaboration outcome](docs/collaboration/05_OUTCOME.md), [frontend plan](docs/plans/FRONTEND_FOUNDATION_v1.md) and [archive index](archive/README.md) | Ownership and prior decisions. The published checkout also has `meeting/05_OUTCOME.md`. The archived MyComunity plan, PDF and older migration context provide provenance. |

## 4. Published implementation checkpoint

The published backend uses **Spring Boot 4.1.2**, Java 17 source, Java 21
for the verified run, Maven 3.9, PostgreSQL 16, Flyway and JUnit. Java source
starts at `backend/src/main/java/my/baiti/`. React, TypeScript and Vite
remain the frontend plan. The inspected revision has no frontend source tree
or GitHub Actions workflow files.

| Commit | Published change |
|---|---|
| `288d224` | 42-code tenant permission catalogue, default role grants, sync that preserves revocations and custom roles, named authority enforcement and matching-tenant actor stamps |
| `e87a98b` | Resident ownership predicates, active unit access and neutral 404 for foreign or unknown resources |
| `d8853e4` | Separate platform sign-in, tenant listing and atomic onboarding of tenant, grants, initial admin and audit |
| `8026635` | First-use password replacement and transactional refresh revocation. Existing access tokens missing the password-state claim need reissue. |
| `4c74e61` | Tenant authentication events through a bounded asynchronous worker that binds and clears its tenant. Pending in-process events can be lost on abrupt shutdown. |
| `943b885` | I1–I9, V32 with 28 tenant-aware foreign keys and V33 checking existing references under FORCE RLS |
| `d5ed66d` | Opt-in dev seeds through normal onboarding and a two-community authentication smoke |

Inspect these seams when reconciling the implementation.

- **Tenant boundary.** Read `tenancy/`, `config/TenantAwareDataSource.java`
  and datasource configuration. Resolve the tenant before auth, require the
  signed tenant to match, and maintain the Hibernate, connection and
  PostgreSQL `app.current_tenant` boundary. Runtime uses non-owner,
  non-bypass `baiti_app`. Flyway and the bounded platform path use
  `baiti_migrator`.
- **Auth and onboarding.** `security/AuthController.java` exposes
  `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/refresh`,
  `POST /api/auth/logout` and `POST /api/auth/password`. The separate
  `tenancy/admin/` plane exposes `POST /api/admin/auth/login` and
  `GET/POST /api/admin/tenants`. Token audiences and principal types
  separate the planes. Initial passwords are replaced before full access.
- **Permissions and audit.** Follow `people/PermissionCatalog.java`,
  `people/PermissionSyncService.java`, `security/ResidentScopeGuard.java`,
  `system/TenantActivityWriter.java` and `tenancy/TenantActivityListener.java`.
  Verify the applicable authority and ownership. Resolve parents under the
  caller's tenant scope before writing and use the same neutral 404 for a
  foreign and an unknown resource. Keep the database constraints as a
  second boundary.
- **Schema and product status.** `backend/src/main/resources/db/migration/`
  contains V1–V33 for 26 authored application relations. V32 prevents new
  cross-tenant references and V33 identifies an invalid legacy reference by
  constraint name. Use the documented repair plan for a failing upgrade.
  Property CRUD, resident verification, finance, commerce and safety
  journeys remain future work at this checkpoint. Existing entities,
  repositories and test-only routes provide foundations.

## 5. Native verification and workstation setup

**Dated evidence from 8 November 2026.** PostgreSQL 16.15 passed **151
unit/architecture + 170 integration tests**, with zero failures, errors or
skips. I1–I8 ran in `TenantIsolationIT`, I9 in `PlatformAuthIT`, and the
permission matrix in `PermissionMatrixIT`. A separate empty database
applied V1–V33 and passed seven `MigrationIT` checks. The separate dev
database proved first-use password replacement, sign-in, identity, refresh
rotation, logout, cross-tenant denial and seed idempotency for two fictional
communities with curl. Local changed-line coverage was 91.3% of executable
Java lines since P04. Read the dated implementation records under
`docs/ai-context/06_TESTING_EVIDENCE/`.

- `2026-11-08_tenant_isolation.md` covers the full suite, fresh migration,
  legacy-reference rejection and rollback drill.
- `2026-11-08_dev_authentication.md` covers the dev smoke, including the
  initial rate-limited request and the completed retry.
- `2026-11-08_resident_scope.md`, `2026-11-08_platform_onboarding.md`,
  `2026-11-08_first_use.md` and `2026-11-08_auth_audit.md` record the
  individual security slices.

Re-establish evidence on the new workstation when verification is in scope.

1. Check Git, PowerShell, Java and Maven. Use Java 21 for parity unless a
   newer runtime is verified in the project. Discover PostgreSQL before
   assuming its service, port, version or database names. The previous
   loopback port 55432 and `backend/.local/postgres16/` are historical.
2. Follow `docs/testing/NATIVE_POSTGRES.md` for an explicitly owned
   PostgreSQL 16 test target. Use a dedicated `baiti_test_*` database,
   `baiti_migrator` as database and public schema owner, a separate
   `baiti_app` login with restricted attributes, and the matching database
   comment `baiti-test:<UUID>`. Follow the documented migration and
   synthetic fixture order, including `foundation-tenants.sql` and
   `foundation-platform.sql`.
3. Supply the seven test settings through the process environment or an
   ignored private settings file. They are `BAITI_TEST_DB_MODE=native`,
   `TEST_DB_URL`, `TEST_DB_APP_USER`, `TEST_DB_APP_PASSWORD`,
   `TEST_DB_MIGRATOR_USER`, `TEST_DB_MIGRATOR_PASSWORD` and
   `TEST_DB_OWNER_TOKEN`. From the implementation root, run this after
   ownership and role preflight.

   ```powershell
   ./tools/testing/Invoke-NativeTests.ps1 -SettingsPath '<private-test-settings.json>'
   ```

   The runner defaults to offline Maven. Use its `-Online` option for normal
   pinned dependency resolution if the fresh machine needs it and network
   access is authorised. Capture the exit code, counts, failures and
   cleanup evidence. Keep missing dependencies and tests that did not run
   visible in the report.
4. Use a **separate** owned loopback `baiti_dev_*` database for dev seeds.
   Read the published `docs/ai-context/04_DATABASE_STATE/seeders_status.md`.
   Opt-in needs a matching `baiti-dev:<UUID>` marker, an existing active
   synthetic platform operator, a private initial password and the `dev`
   profile. Keep local credentials private and use fictional fixture data.

## 6. Transfer checklist

- **Single checkout.** For a workstation move, transfer or restore `baiti`
  as one project folder. Back up and verify local planning and working-tree
  edits before changing branches or replacing files. Use `igumi` for
  Faisal's development. Existing Git branches provide the isolation for
  implementation work.
- **Older transfers.** Treat detached copies and old worktree metadata as
  historical evidence. Reconcile those materials into the canonical checkout
  and archive conflicting originals before retiring a duplicate checkout.- **Shared guidance and runtime.** Preserve access to the workspace-owned
  `standards/`, `core/rules/`, `core/skills/` and secret scanner when
  needed. In a standalone clone, continue discovery with project rules and
  report missing shared guidance. Re-create private settings and disposable
  databases where practical. Use PostgreSQL-aware backup and restore for
  any database that genuinely needs transfer. Keep unrelated projects,
  credentials and generated output out of the handoff.

## 7. Working rules and the next slice

- **Branch model.** `main` is production and PR target, `igumi` is
  Faisal's author branch, and `anep` is Anep's. The 8 November alignment
  was owner-requested. Author branches can diverge afterwards. Use a
  meaningful PR per reviewable outcome and keep these three branches unless
  the owner directs otherwise. Push, merge, another alignment and deployment
  require authority for the current action.
- **Commit SOP.** Read `standards/COMMIT_MESSAGES.md` and the canonical
  Humanizer guide. Use the `Type(Scope): Sentence-case title.` subject and
  `Project`, `Date`, `Task` and `Verified that` sections. Write concrete
  UK English, cite actual checks, stage specific files and run active hooks,
  secret and whitespace checks. The recorded owner direction allows verified
  local implementation commits without a separate message approval pause.
  The latest request governs the scope of that work.
- **Implementation.** Preserve unrelated work. Reproduce a failure before
  fixing it. Run native PostgreSQL checks for database-dependent behaviour.
  Keep tenant isolation, app/migrator separation, immutable applied
  migrations and exact money using NUMERIC/BigDecimal. Update current
  status and dated evidence after meaningful work.
- **Next backend work.** P05 has local G-RLS evidence. When authorised,
  re-establish local prerequisites and begin P08 property backend. Read
  `PACKET_08_property.md`, FR-PROP-01–04 and the permission matrix.
  Trace block/unit CRUD, scoped 404s, deletion and uniqueness 409s, filtered
  and sorted unit lists, and the slim Guard projection. Manual occupancy
  belongs to P08. Automatic residency transitions follow in P09.
- **Remaining gates.** Use the native test contract for P06 GitHub
  verification. Check the collaboration record for P07 ownership and
  acceptance. Frontend journeys, hosted CI and deployment require their
  own evidence. Finance, integrations, commerce and safety also depend on
  their product and operational decisions.

## 8. Return one resume report

1. Canonical checkout root, branch and revision, dirty
   scope, worktrees and live GitHub alignment if verified.
2. Confirmed pilot decisions, architecture, implementation and open product
   decisions, each tied to its governing source.
3. A file map separating published, local or transferred, archival and
   unavailable material. Cite the dates of meaningful conflicts.
4. What was verified on the new machine, what comes from prior evidence
   and what still needs an external or runtime check.
5. The next bounded task, acceptance checks and any blocker that genuinely
   needs owner input. Continue independent work within scope.
