# P02 — Backend Skeleton

**Objective**: A Spring Boot 3 / Java 17 Maven project under `backend/` that boots, exposes health, runs one green Testcontainers test, and carries all quality tooling — before any business code exists.

**Scope**
- In: Maven project, profiles, dual-datasource configuration shell, quality plugins, health test.
- Out: migrations (P03), entities (P04), security (P05) — `/actuator/health` is permitted anonymously for now with a `// P05 locks this down` note.

**Dependencies**: P01.

**Files to create**
```
backend/
├── pom.xml                     # parent spring-boot-starter-parent 3.x
├── .mvn/ + mvnw, mvnw.cmd      # wrapper committed
├── src/main/java/my/baiti/BaitiApplication.java
├── src/main/java/my/baiti/config/DataSourceConfig.java   # app pool (baiti_app) + flyway datasource (baiti_migrator)
├── src/main/resources/application.yml
├── src/main/resources/application-dev.yml
├── src/main/resources/application-test.yml
├── src/main/resources/application-prod.yml
└── src/test/java/my/baiti/HealthIT.java                  # Testcontainers context+health test
```

**Expected architecture**
- Dependencies: web, data-jpa, security, validation, flyway-core (+flyway-database-postgresql), actuator, postgresql driver, lombok, springdoc-openapi, testcontainers (junit-jupiter, postgresql), spring-security-test, archunit-junit5 (test), micrometer-registry-prometheus, bucket4j (may defer to P05).
- Plugins: surefire/failsafe split (`*IT` = integration), Spotless (google-java-format) bound to `check`, JaCoCo with 70 % changed-line rule (activated fully in P06 CI), OWASP dependency-check (CI profile — slow locally).
- Config (03_ARCHITECTURE §8): `spring.datasource.*` ← `DB_APP_USER` (Hikari runtime); `spring.flyway.user/password` ← `DB_MIGRATOR_*`; `jpa.hibernate.ddl-auto: validate` in ALL profiles; `flyway.locations: classpath:db/migration`; actuator exposes `health,info` only; prod profile has **no defaults** for secrets (missing env = startup failure, by design).
- Security starter present ⇒ everything except `/actuator/health` 401s by default — acceptable at this packet.

**Testing requirements**
- `HealthIT`: singleton `PostgreSQLContainer("postgres:16")` pattern (06_TESTING §1 speed budget), `@DynamicPropertySource` wiring both datasource + flyway props; asserts context loads and `GET /actuator/health` = 200 `UP`. (No migrations exist yet — Flyway on an empty location must be configured to not fail: keep `db/migration` folder with a `.gitkeep` and `flyway.fail-on-missing-locations: false` until P03 lands, then remove the relaxation in P03.)

**Definition of Done**
- [ ] `./mvnw verify` green including `HealthIT` (evidence: output captured to `docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md`).
- [ ] `./mvnw spring-boot:run` with compose DB up boots on :8080, health UP, and the log line proves the runtime pool user is `baiti_app` (log `current_user` at startup once, DEBUG).
- [ ] Spotless check passes; wrapper committed; no secrets anywhere (`.env` consumed via environment, not committed).
- [ ] Commit(s) per protocol.
