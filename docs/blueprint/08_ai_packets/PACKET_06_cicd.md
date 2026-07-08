# P06 — CI/CD Foundation

**Objective**: Every PR proven by machines: build, tests (including migration + isolation suites), quality gates, security scans. CD stubs prepared, activated at M6.

**Scope**
- In: two CI workflows, Dependabot, secret scanning, OpenAPI artifact job, branch-protection documentation.
- Out: actual deployment jobs (P18); the GitHub repo itself exists only after **G-PUSH** — until then workflows are authored and validated locally (`act` optional) and this packet's DoD is "ready + documented", flipping to "green on PR" the day the remote exists.

**Dependencies**: P05 (a CI without the isolation suite would certify nothing). **Gate**: G-PUSH for anything remote.

**Files to create**
```
.github/workflows/ci-backend.yml     .github/workflows/ci-frontend.yml
.github/dependabot.yml               .gitleaks.toml (or git-secrets config)
docs/deployment/BRANCH_PROTECTION.md (settings to apply the day the repo exists)
docs/deployment/CD_PLAN.md           (stub stages per 03 §12, activated in P18)
```

**Expected architecture**
- `ci-backend.yml` (paths: `backend/**`): JDK 17 (Corretto) + `~/.m2` cache → `./mvnw verify` (unit+IT via Testcontainers — ubuntu runners have Docker) → JaCoCo report + 70 % changed-lines check → OWASP Dependency-Check (fail high/critical) → upload OpenAPI artifact from springdoc build step.
- `ci-frontend.yml` (paths: `frontend/**`, added when P07 lands; author now, `if: hashFiles('frontend/package.json')` guard): Node LTS + cache → `npm ci` → lint → typecheck → `npm test` → `npm run build` → `npm audit --audit-level=high`.
- Both: gitleaks step; concurrency group per-branch; `workflow_dispatch` enabled.
- Dependabot: weekly, maven + npm + github-actions ecosystems.
- `BRANCH_PROTECTION.md`: `main` + `dev` require PR, 1 approval, both workflows as required checks, no force-push, linear history.

**Testing requirements**: workflow YAML validated (actionlint or dry parse); once remote exists — a `test: ci wiring` PR shows both workflows green and required.

**Definition of Done**
- [ ] Workflows + configs authored, lint-clean, committed (user-approved messages).
- [ ] `BRANCH_PROTECTION.md` and `CD_PLAN.md` complete enough that applying them is mechanical.
- [ ] Post-G-PUSH addendum executed when possible: test PR green, protection applied, screenshot/output evidence filed.
