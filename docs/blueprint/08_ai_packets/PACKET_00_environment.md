# P00 — Environment Verification & Provisioning

**Objective**: A verified Windows dev environment able to build and test both stacks; evidence on file. Nothing else in the programme may start before this.

**Scope**
- In: verify/install Java 17 (Corretto), Maven, Node LTS (18/20+), Docker Desktop, git, GitHub CLI; confirm Java 8 coexistence for sibling projects (per-project `JAVA_HOME`).
- Out: any file in the repo other than the verification log; any code.

**Dependencies**: none.

**Files to create**
- `docs/audits/PHASE0_VERIFICATION.md` — raw command outputs + install actions taken.

**Expected procedure**
1. Run and capture: `java -version` · `mvn -v` · `node -v` · `npm -v` · `docker ps` · `git --version` · `gh auth status`.
2. For each missing/old tool, install (`winget install Amazon.Corretto.17`, `Apache.Maven`, `OpenJS.NodeJS.LTS`, `Docker.DockerDesktop`, `GitHub.cli`) or record the manual-installer fallback used; re-run the check.
3. Set a per-project `JAVA_HOME` strategy (do not change the global default if Java 8 projects depend on it); verify a sibling Laravel/Java project still functions if applicable.
4. `docker run --rm hello-world` to prove the daemon actually runs (Docker Desktop on Windows frequently installs but doesn't start).

**Testing requirements**: the captured outputs ARE the test. Every command must show the expected minimum version.

**Definition of Done**
- [ ] All seven commands succeed with: Java ≥17, Maven ≥3.9, Node ≥18, Docker daemon responding, git present, `gh` authenticated (or explicitly deferred with G-PUSH note).
- [ ] `PHASE0_VERIFICATION.md` committed-ready with verbatim outputs and dates.
- [ ] Any tool that could not be installed is documented as a blocker with the attempted commands — do not proceed to P01 with missing Java/Maven/Node/Docker.
