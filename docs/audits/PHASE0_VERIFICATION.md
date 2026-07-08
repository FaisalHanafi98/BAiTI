# BAiTI — Phase 0 Environment Verification (Packet P00)

> **Run**: 2026-07-07, Windows 11, Git Bash + PowerShell
> **Verdict**: PASS with one deferral — Docker daemon startup deferred by user (Docker Desktop unresponsive today). Docker is first *required* at P02 (Testcontainers); P01 authoring proceeds, its compose verification is deferred with it.

## Captured outputs (verbatim)

```
=== java ===
openjdk version "21.0.11" 2026-04-21 LTS
OpenJDK Runtime Environment Temurin-21.0.11+10 (build 21.0.11+10-LTS)
=== mvn ===  (after install, see below)
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: C:\Users\asbou\tools\apache-maven-3.9.16
Java version: 21.0.11, vendor: Eclipse Adoptium
=== node ===
v24.18.0
=== npm ===
11.16.0
=== docker ===
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
=== git ===
git version 2.54.0.windows.1
=== gh ===
✓ Logged in to github.com account FaisalHanafi98 (keyring)
  Token scopes: 'gist', 'read:org', 'repo', 'workflow'
```

## Requirement vs reality

| Tool | Required | Found | Status |
|---|---|---|---|
| Java | 17+ | **Temurin 21.0.11 LTS** | ✓ (exceeds; Spring Boot 3.x supports 21 — note: plan expected Corretto 17, Temurin 21 is present instead; fine) |
| Maven | 3.9+ | **3.9.16** — installed this session | ✓ |
| Node | 18+ | v24.18.0 | ✓ |
| npm | — | 11.16.0 | ✓ |
| Docker | daemon running | Docker Desktop **4.78.0 installed**, daemon not responding; `Start-Process` and startup attempts produced no process | ⚠ **DEFERRED (user decision 2026-07-07)** |
| git | any | 2.54.0 | ✓ |
| gh | authenticated | FaisalHanafi98, repo+workflow scopes | ✓ |

## Install actions taken

1. **Maven**: `winget install Apache.Maven` FAILED — the package does not exist in winget (the 2026-04-08 plan's assumption was wrong; correction for future docs). Manual install instead: downloaded `apache-maven-3.9.16-bin.zip` from `https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/`, extracted to `C:\Users\asbou\tools\apache-maven-3.9.16`, appended `…\bin` to the **user** PATH (persisted). Verified: output above.
2. **Docker**: launch attempts (Docker Desktop.exe via Start-Process, twice) spawned no process; further attempts stopped at user request.

## Outstanding before P02

- [ ] Docker Desktop starts and `docker ps` succeeds (try: launch from Start Menu; if it hangs, check WSL2 health `wsl --status`, or Settings → "Use the WSL 2 based engine"; consider updating 4.78 → 4.81 which winget lists as available).
- [ ] Then: `docker compose up -d` in `infra/` and the P01 role-verification queries (see `infra/db-init/`).

## Java note for sibling projects

Temurin 21 is the machine default (`java -version`). If any sibling project needs Java 8, set a per-project `JAVA_HOME` there; BAiTI itself targets 17+ bytecode (`maven.compiler.release=17`) and runs fine on 21.
