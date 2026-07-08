# P01 — Repo Bootstrap & Governance

**Objective**: A local git repository named and governed for BAiTI: monorepo skeleton, agent rules, env-var contract, local database infrastructure with the **two-role bootstrap** (this is where RLS enforceability is born), and the first user-approved commit.

**Scope**
- In: folder rename (with user confirmation — gate G-NAME), `git init`, governance files, docs tree, docker-compose dev DB, ADRs 0001/0002.
- Out: any Java/TS code; GitHub remote (G-PUSH); backend/frontend scaffolds (P02/P07).

**Dependencies**: P00. **Gate**: G-NAME — ask the user to confirm renaming `Anep's Project (Baiti)` → `baiti` before executing.

**Files to create**
```
baiti/
├── .gitignore                  # Java (target/), Node (node_modules/, dist/), IDE, .env*, *.local
├── .env.example                # exactly the inventory in 03_ARCHITECTURE §8, each var commented
├── README.md                   # what BAiTI is, stack, quickstart pointers, blueprint pointer
├── CLAUDE.md                   # project agent rules (content spec below)
├── AGENTS.md                   # same rules for non-Claude agents (may @-reference CLAUDE.md)
├── infra/
│   ├── docker-compose.yml      # postgres:16 + pgadmin
│   └── db-init/01_roles.sql    # role bootstrap (below)
├── docs/{adr,audits,architecture,database,testing,security,deployment,specs,plans}/.gitkeep
├── docs/adr/0001-project-naming.md      # MyComunity→BAiTI history, folder/repo/package/class choices
└── docs/adr/0002-monolith-monorepo.md   # summarised from 03_ARCHITECTURE §1
```
(Existing `docs/blueprint/`, `docs/ai-context/`, `docs/audits/`, `meeting/`, legacy PDF/plan files are kept in place and enter version control as-is.)

**Expected architecture**
- `docker-compose.yml`: service `db` = `postgres:16`, env from `.env` (`POSTGRES_DB=baiti`, superuser creds), port 5432, healthcheck `pg_isready`, volume; service `pgadmin` optional profile. Mount `db-init/` at `/docker-entrypoint-initdb.d/`.
- `db-init/01_roles.sql` (runs once at first container start; uses psql var substitution from env — never hard-code passwords):
  ```sql
  CREATE ROLE baiti_migrator LOGIN PASSWORD :'mig_pw';
  CREATE ROLE baiti_app LOGIN PASSWORD :'app_pw' NOBYPASSRLS;
  ALTER DATABASE baiti OWNER TO baiti_migrator;
  GRANT USAGE ON SCHEMA public TO baiti_app;
  ALTER SCHEMA public OWNER TO baiti_migrator;
  ```
  (Wire env → psql vars via a small wrapper script; document in the file header. Prod bootstrap uses the same SQL manually — note it in `docs/deployment/`.)
- `CLAUDE.md` (project-level, LEVEL 2 under the root SOP) must contain, concisely: purpose + current state; stack; **source-of-truth pointer to `docs/blueprint/`**; non-negotiables (multi-tenant isolation mandatory; RLS proven before feature work; TDD; no secrets; commit protocol CON-4 — draft → user edits → commit, never push; never edit applied migrations; deviation register 04 §9 is normative); allowed commands (`mvn verify`, `npm test`, `docker compose up -d`, …); forbidden actions; `docs/ai-context/` update duties; evidence rules.

**Testing requirements**
- `docker compose up -d` → `docker compose ps` healthy; `psql -U baiti_app -d baiti -c "select current_user"` connects; `psql` as `baiti_app`: `SELECT rolbypassrls FROM pg_roles WHERE rolname='baiti_app'` = false. Capture outputs.
- `git log --oneline` shows the initial commit(s) with user-approved message.

**Definition of Done**
- [ ] Folder renamed with user confirmation; ADR-0001 records it.
- [ ] `git init` on branch `main`; `.gitignore` proven by `git status` (no `.env`, no junk).
- [ ] Compose DB up with both roles verified NOBYPASSRLS-correct.
- [ ] CLAUDE.md/AGENTS.md exist with the mandated content; `.env.example` matches 03 §8 exactly.
- [ ] First commit executed only after user edited/approved the drafted message; **no remote configured**.
