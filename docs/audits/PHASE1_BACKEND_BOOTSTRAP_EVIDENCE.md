# P02 — Backend Skeleton Bootstrap Evidence

> **Packet**: P02 (backend skeleton) — `docs/blueprint/08_ai_packets/PACKET_02_backend_skeleton.md`
> **Date**: 2026-07-08
> **Agent**: OpenCode (glm-5.2)
> **Status**: **AUTHORED — partial verification.** Docker daemon is unavailable on the dev machine
> (installation problem; user deferred 2026-07-08). All Docker-gated verification is therefore
> pending. Non-Docker verification (POM resolution, Spotless, compilation) is GREEN.

---

## 1. Files created (P02 scope)

```
backend/
├── pom.xml
├── mvnw, mvnw.cmd
├── .mvn/wrapper/maven-wrapper.properties   (Maven 3.9.9, only-script distribution)
├── src/main/java/my/baiti/BaitiApplication.java
├── src/main/java/my/baiti/config/DataSourceConfig.java     (dual-role: baiti_app pool + Flyway migrator)
├── src/main/java/my/baiti/config/SecurityConfig.java       (/actuator/health permitAll; P05 replaces)
├── src/main/java/my/baiti/config/StartupDiagnostics.java   (logs current_user at boot — DoD support)
├── src/main/resources/application.yml        (base: ddl-auto validate, flyway, actuator health/info)
├── src/main/resources/application-dev.yml     (dev defaults + optional .env import)
├── src/main/resources/application-test.yml    (structural; IT supplies connection props)
├── src/main/resources/application-prod.yml    (NO secret defaults — missing env = startup fail)
├── src/main/resources/db/migration/.gitkeep   (Flyway empty-location placeholder until P03)
└── src/test/java/my/baiti/HealthIT.java       (singleton PG16 container, dual-role, health UP)
```

### Files added beyond the packet's explicit list (justified)
- `SecurityConfig.java` — required to achieve the packet's stated behaviour ("everything except
  `/actuator/health` 401s"). Without it, Spring Security's default secures health too (401). Marked
  `// P05 locks this down`.
- `StartupDiagnostics.java` — supports the DoD "the log line proves the runtime pool user is
  `baiti_app`". Emits one DEBUG line at boot with `SELECT current_user` + the tenant GUC.

## 2. Dependency & plugin manifest (pom.xml)

- **Parent**: spring-boot-starter-parent 3.3.5 · **Java**: 17 (runs on Temurin 21).
- **Starters**: web, validation, actuator, security, data-jpa.
- **Migrations**: flyway-core + flyway-database-postgresql; postgresql driver (runtime).
- **API/docs**: springdoc-openapi-starter-webmvc-ui 2.6.0.
- **Observability**: micrometer-registry-prometheus.
- **Lombok** (optional/provided).
- **Test**: spring-boot-starter-test, spring-security-test, testcontainers (junit-jupiter, postgresql),
  archunit-junit5 1.3.0.
- **Deferred**: bucket4j → P05 (auth/rate-limit module), per packet "may defer to P05".
- **Plugins**: spring-boot-maven-plugin (repackage); surefire (excludes `*IT`) + failsafe (includes
  `*IT`, bound to integration-test/verify); spotless-maven-plugin 2.43.0 (google-java-format 1.21.0,
  bound to `validate`); jacoco 0.8.12 (prepare-agent + report; 70 % changed-line rule deferred to P06
  CI); owasp dependency-check 10.0.4 in `ci` profile (failBuildOnCVSS=7).

## 3. Dual-datasource design (ED-4 / 03_ARCHITECTURE §6.2)

- **App pool** (`baiti_app`, NOBYPASSRLS): explicit `@Primary` HikariCP `DataSource` bean in
  `DataSourceConfig`, built from `spring.datasource.*` so P05 can wrap it with
  `TenantAwareDataSource`. RLS binds here.
- **Flyway** (`baiti_migrator`, schema owner): NOT a bean — Spring Boot's Flyway auto-config creates a
  dedicated connection from `spring.flyway.url/user/password` (set in every profile YAML). When
  `spring.flyway.url` is present, Flyway ignores the primary DataSource and uses the migrator
  credentials directly. Owner is exempt from RLS — the two roles must never collapse.
- `jpa.hibernate.ddl-auto: validate` in **all** profiles. `flyway.fail-on-missing-locations: false`
  until P03 (no migrations yet). Prod profile carries **no** secret defaults.

## 4. Verification — VERIFIED this session (no Docker needed)

| Check | Command | Result |
|---|---|---|
| Maven / Java toolchain | `mvn -v` | Apache Maven 3.9.16, Java 21.0.11 (Eclipse Adoptium) — VERIFIED |
| POM resolves (parent + all deps/plugins from Central) | `mvn test-compile` (first run) | deps downloaded, no resolution errors — VERIFIED |
| Spotless (google-java-format) check | `mvn test-compile` → `spotless:2.43.0:check` | "5 files clean — 0 needs changes" — VERIFIED |
| Main sources compile (4 classes) | `mvn test-compile` | `target/classes/my/baiti/{BaitiApplication,config/*}.class` produced — VERIFIED |
| Test sources compile (HealthIT) | `mvn test-compile` | `target/test-classes/my/baiti/HealthIT.class` produced — VERIFIED |
| Resources on classpath | build output | `application*.yml` + `db/migration/.gitkeep` copied to `target/classes` — VERIFIED |
| Final status | `mvn test-compile` | `BUILD SUCCESS` — VERIFIED |

Captured command output:
```
[INFO] --- spotless:2.43.0:check (default) @ baiti-backend ---
[INFO] Spotless.Java is keeping 5 files clean - 0 needs changes ...
[INFO] Compiling 4 source files with javac [debug parameters release 17] to target\classes
[INFO] BUILD SUCCESS
```

## 5. Verification — UNVERIFIED (blocked on Docker daemon)

These P02 DoD items require the Docker daemon, which is currently unavailable. They remain open and
must be closed before P02 is declared done. **Run once Docker is fixed:**

| # | DoD item | Command to run | Expected |
|---|---|---|---|
| U1 | HealthIT green (Testcontainers PG16) | `./mvnw verify` (or `./mvnw -Dtest=HealthIT failsafe:integration-test`) | context loads + `GET /actuator/health` = 200 UP; proves dual-role wiring boots |
| U2 | App boots on :8080, health UP, pool user = baiti_app | start Docker → `docker compose -f infra/docker-compose.yml up -d` → `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` → `curl localhost:8080/actuator/health` | 200 UP; DEBUG log line "pool current_user=baiti_app" |
| U3 | Compose role checks (P01 residue) | `docker exec baiti-db psql -U postgres -d baiti -c "SELECT rolname, rolbypassrls FROM pg_roles WHERE rolname IN ('baiti_app','baiti_migrator');"` | `baiti_app` rolbypassrls = `f`; `baiti_migrator` = owner |
| U4 | Spotless/JaCoCo under full verify | `./mvnw verify` | BUILD SUCCESS; JaCoCo report generated (threshold rule lands in P06) |

## 6. TDD note

The "failing test first → watch it pass" cycle for `HealthIT` could not be observed because the test
requires a running PostgreSQL container (Docker). The test was written against the packet spec and is
compiled, but it has **not been executed**. This is recorded as an accepted, Docker-driven deferral —
not a skipped quality step. The first `./mvnw verify` once Docker is available is the real
failing→passing observation point.

## 7. Decision: bucket4j deferred to P05

Per the packet ("bucket4j may defer to P05"), the rate-limiting library is not on the P02 classpath.
It lands with the auth module (P05b), which is its sole consumer. Recorded here for traceability.
