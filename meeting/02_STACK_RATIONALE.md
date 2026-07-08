# MyComunity — Tech Stack Rationale

> **Purpose**: Convince Anep (and yourself) that the chosen stack is the right call. Use this in the meeting if he questions a choice. Each section ends with a one-line summary you can say out loud.

---

## TL;DR

**Spring Boot 3 (Java 17, Maven) + PostgreSQL 16 + React 18 (TypeScript, Vite, Tailwind) + Multi-tenant SaaS architecture from day 1.**

This stack is what serious condo management products are built on. It scales, it's hireable, it ports to mobile, and it's secure-by-default for multi-tenant SaaS.

---

## 1. Why Spring Boot (backend)

### What it is
Java's most popular web framework. Built by Pivotal/VMware. Used by every major bank, telco, government system in Malaysia (Maybank, RHB, Touch 'n Go, MyEG, LHDN systems). Enterprise standard.

### Why for MyComunity specifically
1. **Type safety**: Java's strict typing catches whole classes of bugs at compile time. For a 22-table schema with FK relationships everywhere, this matters.
2. **JPA / Hibernate**: ORM that maps Java classes to database tables — same idea as Laravel Eloquent but more powerful. Handles complex relationships (one-to-many, many-to-many, self-referential) cleanly.
3. **Spring Security**: Industry-standard auth/authz. JWT, RBAC, method-level permission checks. Not a hand-rolled solution.
4. **Mature multi-tenancy support**: Hibernate has built-in tenant filters; combined with PostgreSQL RLS we get bulletproof isolation.
5. **Rich testing ecosystem**: JUnit 5 + Testcontainers lets us test against a real PostgreSQL in CI, not mocks.
6. **Hireability**: Spring Boot on a CV opens doors at every enterprise in KL/Singapore.

### Why not Laravel?
- I already have Laravel on my CV via CREAMS. Adding another Laravel project = portfolio repetition.
- Spring Boot is more aligned with where I want to grow professionally (enterprise dev, financial systems).
- For Anep: Java is broadly more in-demand in Malaysia for graduate-level enterprise jobs.

### Why not Node.js / Express?
- We'd lose static typing without TypeScript on the backend, and adding TS to Node is more setup than picking Java directly.
- Node's ecosystem moves too fast — enterprise SaaS prefers stable LTS frameworks.
- JPA has no real Node equivalent for complex relational models. Prisma is good but not at the same maturity for 22-table apps with rich auditing.

### Why not Django / FastAPI (Python)?
- Multi-tenant story is weaker out of the box.
- Slower in-VM execution than JVM for compute-heavy paths (less critical here, but matters at scale).
- Anep and I have less Python experience.

> **One-liner for the meeting**: *"Spring Boot is what every bank in Malaysia runs on. It handles 22 tables, RBAC, and JWT auth out of the box, and it's what I want on my CV next."*

---

## 2. Why PostgreSQL (database)

### What it is
The most powerful open-source relational database. Used by Apple, Spotify, Reddit, Instagram, NASA, every fintech you can name.

### Why for MyComunity specifically
1. **Row-Level Security (RLS)**: PostgreSQL can enforce "user X can only see rows where tenant_id = X" at the database layer. **This is non-negotiable for multi-tenant SaaS.** Even if our application code has a bug, the database refuses to leak data across condos.
2. **JSONB columns**: For flexible fields (e.g. complaint metadata, notification payloads) we get JSON queries with B-tree indexing.
3. **UUID primary keys**: Native UUID type. No sequential IDs that leak business info ("you have 47 condos, that's nothing").
4. **Strong consistency + ACID**: Financial data (bills, payments) must never be lost or duplicated. PostgreSQL is bulletproof here.
5. **Free, no licensing surprises**: Unlike Oracle, MS SQL Server, or even MySQL (Oracle-owned, license traps).

### Why not MySQL / MariaDB?
- No native RLS. Application-only tenant filtering = one bug from a leak.
- Weaker JSON support.
- Smaller advanced feature set (CTEs, window functions, partial indexes).

### Why not MongoDB?
- 22 tables with FK relationships = relational data. Forcing it into documents is fighting the model.
- No real transactions across collections (until recently, and still painful).

> **One-liner**: *"PostgreSQL has built-in row-level security. For multi-tenant, that's the difference between safe and one-bug-from-disaster."*

---

## 3. Why React + TypeScript + Vite (frontend)

### What it is
React = UI library by Meta, by far the most popular frontend tool in the world.
TypeScript = JavaScript with types. Catches errors before runtime.
Vite = the modern build tool. Replaces webpack. 100x faster.

### Why for MyComunity specifically
1. **Mobile portability**: React Native uses the same language, same hooks, same patterns. **When we ship the mobile app, our auth flow, API client, types, and business logic come with us.** UI gets rewritten for native, but the brain is shared.
2. **Type safety end to end**: TypeScript on the frontend + auto-generated types from the backend's OpenAPI spec = if I rename a field on the backend, the frontend stops compiling. No silent breakage.
3. **Component model**: Reusable building blocks. The 22-table app has lots of repeating UI patterns (forms, tables, modals) — components let us build once, use everywhere.
4. **TanStack Query**: Manages server state (loading, caching, refetching) without writing useEffect spaghetti. Cuts our frontend complexity in half.
5. **Vite**: Dev server starts in <1 sec, hot reload is instant. Way better DX than CRA or webpack-based setups.
6. **Hireability**: React is the #1 frontend skill on every job post in Malaysia.

### Why not Vue?
- Smaller ecosystem, fewer libraries, less mobile portability.
- Not a deal-breaker, but React wins on team familiarity and React Native path.

### Why not Angular?
- Too heavy for an MVP. Bigger learning curve. Slower to iterate.
- Older enterprise feel. Not where the industry is moving.

### Why not Next.js?
- We don't need SSR/SSG. This is a CRUD app behind login.
- Vite + React Router is simpler and the right tool for the job.

> **One-liner**: *"React because we're going to ship a mobile app next, and React Native lets us reuse 80% of the brain — auth, API, hooks, types — when we get there."*

---

## 4. Why multi-tenant from day 1

### What it means
One deployment of MyComunity serves multiple condos. `condoA.mycomunity.app` and `condoB.mycomunity.app` are the same backend, same database, but their data is **completely isolated**.

### Why
1. **Real condo SaaS works this way**. Buildium, AppFolio, JaagaPP — every successful condo platform is multi-tenant. Single-tenant doesn't scale as a business.
2. **Cheaper to do upfront**. Adding `tenant_id` to every table later = nightmare migration. Doing it now = 5 minutes per table.
3. **Sellable / commercial potential**. If we want to actually license this to condos in the future, multi-tenant is the only model that works.
4. **Stronger portfolio piece**. "I built a multi-tenant SaaS with row-level security and three-layer tenant isolation" is a much stronger interview story than "I built a CRUD app."

### How we make it bulletproof — Three-layer defence
| Layer | What it does |
|---|---|
| **1. JWT claim** | Every login token carries `tenantId`. Frontend can't fake it without breaking the token signature. |
| **2. Application context** | Spring Boot reads `tenantId` from JWT, sets it in a `TenantContext` (per-request thread-local). All queries auto-scope. |
| **3. Database RLS** | PostgreSQL row-level security policies enforce `tenant_id = current_setting('app.current_tenant')` on every query. **Even raw SQL or a bug in our code cannot leak data.** |

If layer 1 fails, layer 2 catches it. If layer 2 fails, layer 3 catches it. Three independent failures required for a leak. That's defence in depth.

> **One-liner**: *"Three layers — JWT, app context, database RLS — protect tenant data. A single bug can't leak across condos."*

---

## 5. Why a monorepo (one repo, two modules)

```
MyComunity/
├── backend/    (Spring Boot)
├── frontend/   (React)
└── infra/      (docker-compose)
```

### Why
- Schema, API, and client stay in lockstep. One PR can change all three at once.
- One CI pipeline, one issue tracker, one place to look.
- Industry standard for full-stack apps of this size (Vercel does it, Netlify does it, every startup does it).

### Why not two separate repos?
- API contract drift: backend changes, frontend doesn't know.
- Two PR review processes for what's logically one change.
- Annoying when you need to coordinate releases.

> **One-liner**: *"One repo, one CI, one source of truth. Backend and frontend ship together."*

---

## 6. What we're explicitly NOT using (and why)

| Tool | Why not |
|---|---|
| Microservices | Premature for one team. Monolith first, split later if needed. |
| Kubernetes | Massive overkill. Docker Compose in dev, single VM/PaaS in prod. |
| GraphQL | REST + OpenAPI is simpler and matches mobile better. |
| Redux | TanStack Query + Zustand cover server + client state with way less boilerplate. |
| Webpack | Vite is the modern answer. |
| jQuery | It's 2026. Move on. |
| MongoDB | Wrong tool for relational data. |
| Firebase | Vendor lock-in, hard to multi-tenant correctly, expensive at scale. |

---

## 7. Honest risks (be upfront with Anep)

| Risk | Reality |
|---|---|
| Spring Boot has a learning curve | Yes. ~2 weeks of pain, then productive. I've used it on Portfolio project so I can carry the team through it. |
| Multi-tenant adds upfront complexity | Yes. ~30% more setup work. Pays for itself in week 4. |
| Java is verbose vs PHP/JS | True. Lombok + Spring annotations cut 60% of the boilerplate. |
| Two-language stack (Java + TS) = context switching | Yes. Mitigated by clean separation of backend/frontend. Common in real teams. |
| Hosting Spring Boot is more expensive than PHP | True for cheap shared hosting. Not true for modern PaaS (Railway, Fly.io ~$5/mo for our scale). |

---

## 8. The pitch in three sentences

> "MyComunity is going to be a multi-tenant SaaS for condo management — Spring Boot backend, PostgreSQL with row-level security, React frontend, mobile app coming after web ships. The stack is enterprise-grade, hireable, and lets us reuse 80% of the work when we go to mobile. I've already mapped the schema, the architecture, the CI pipeline, and the rollout plan — I just need your buy-in tonight before I write a single line of code."

That's the meeting in three sentences.
