# Test Commands Reference

> **Last Updated**: 2026-05-10
> **Project Phase**: Pre-code (planning only)

---

## Current State

No tests exist. No build tools installed. Commands below are planned for once the project is scaffolded.

---

## Backend Test Commands (Planned — Phase 2+)

```bash
# Run all tests (unit + integration via Testcontainers)
mvn verify

# Run only unit tests (fast)
mvn test -P unit

# Run with coverage report
mvn verify jacoco:report
# Coverage report: backend/target/site/jacoco/index.html

# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Run single test class
mvn test -Dtest=ComplaintControllerTest

# Run single test method
mvn test -Dtest=ComplaintControllerTest#createComplaint_whenUnauthorized_returns403
```

**Exit Criteria**: `mvn verify` → BUILD SUCCESS, ≥70% coverage on touched files

---

## Frontend Test Commands (Planned — Phase 6+)

```bash
# Install dependencies
npm ci

# Run unit tests (Vitest)
npm run test

# Run tests with coverage
npm run test:coverage

# Type checking
npm run typecheck

# Linting
npm run lint

# Build
npm run build

# Security audit
npm audit --audit-level=high
```

**Exit Criteria**: All commands green, coverage ≥60%

---

## Database Test Commands (Planned — Phase 3+)

```bash
# Start local database
docker compose up -d

# Check Flyway migration status
mvn flyway:info

# Apply migrations
mvn flyway:migrate

# Validate migrations
mvn flyway:validate

# Check via psql
psql -h localhost -p 5432 -U postgres -d mycomunity -c "\dt"
psql -h localhost -p 5432 -U postgres -d mycomunity -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## Phase Exit Verification Commands

| Phase | Command | Pass Criterion |
|---|---|---|
| Phase 0 | `java -version && mvn -v && node -v && docker ps && gh auth status` | All tools present |
| Phase 1 | `git status && git log --oneline` | Repo initialized, first commit |
| Phase 2 | `mvn verify` | BUILD SUCCESS |
| Phase 3 | `mvn verify` + `psql \dt` | All 23 tables exist, RLS on every domain table |
| Phase 5 | `mvn verify` (security tests) | Cross-tenant isolation test green |
| Phase 6 | `npm run build && npm run test` | Both green |
| Phase 7 | GitHub Actions CI | Both workflows green on test PR |

---

## Last Actual Test Run

**No tests have been run.** Project is in planning phase.
