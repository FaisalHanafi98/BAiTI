# BAiTI

Multi-tenant SaaS condominium/community management platform. One deployment serves many communities; each community's data is isolated at three independent layers (JWT claim → application tenant filter → PostgreSQL row-level security).

**Status**: pre-code. The complete engineering blueprint lives at [`docs/blueprint/`](docs/blueprint/README.md) — start there. Implementation proceeds by executing the AI work packets in [`docs/blueprint/08_ai_packets/`](docs/blueprint/08_ai_packets/README.md), currently at **P02 (backend skeleton)**.

## Stack

Spring Boot 3 · Java 17+ · Maven · PostgreSQL 16 (+RLS) · Flyway · React 18 · TypeScript · Vite · Tailwind · TanStack Query · JUnit 5 + Testcontainers · Vitest · GitHub Actions

## Layout

```
backend/    Spring Boot (package root my.baiti)      — arrives at P02
frontend/   Vite + React + TS                         — arrives at P07
infra/      docker-compose dev DB + Dockerfiles
docs/       blueprint/ (source of truth) · adr/ · ai-context/ · audits/ · …
meeting/    collaboration records
```

## Quickstart (dev)

```bash
cp .env.example .env            # fill in passwords + JWT_SECRET
docker compose -f infra/docker-compose.yml up -d
cd backend && ./mvnw spring-boot:run    # after P02
cd frontend && npm install && npm run dev    # after P07
```

## Ground rules

Agents and humans alike: read [`CLAUDE.md`](CLAUDE.md). Highlights: TDD always; tenant isolation is proven by tests before any feature work; no secrets in the repo; commits are drafted → reviewed by Faisal → committed; **never push without explicit instruction**.

## Collaborators

Faisal Hanafi (backend lead) · Anep (frontend lead) — decisions log: `meeting/05_OUTCOME.md`.

*Legacy note: this project was planned under the name "MyComunity"; `MyComunity_PLAN.txt` and the data-model PDF are kept for traceability but are superseded by `docs/blueprint/`.*
