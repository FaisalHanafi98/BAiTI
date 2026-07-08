# MyComunity — Meeting Brief with Anep

> **Date**: Tonight (2026-04-08)
> **Goal**: Align on vision, stack, collaboration model. Walk away with clear next steps.
> **Time budget**: 60–90 min realistic

---

## Open with this (2 min)

> "I've spent the last few days putting together a full SDLC plan for MyComunity — schema, architecture, CI/CD, the works. Before I write any code I want to walk you through it and make sure we're aligned. Three big things I need your input on tonight: **vision**, **stack**, and **how we work together**."

Don't show the full plan. Show the **3 pages** (this brief + stack rationale + pitch). Offer the full plan as a follow-up if he wants to dig in.

---

## 1. Vision alignment (15 min) — most important

The data model PDF you sent suggests a real condo management system. I want to confirm a fundamental question:

**Is MyComunity a system for ONE condo, or a SaaS platform that serves MANY condos?**

This is not a small detail. It changes:
- How the database is structured (every table needs a `tenant_id`)
- How users log in (which condo are they from?)
- How we eventually sell or deploy it
- How much work the foundation takes

**My recommendation: SaaS / multi-tenant from day 1.** Reasons:
- Real condo management products (e.g. Buildium, AppFolio, JaagaPP) are all multi-tenant
- It's massively cheaper to build it right at the start than retrofit it later
- It opens the door to a real product / income stream
- It's a stronger portfolio piece for both of us

**Decision needed tonight**: single-condo or multi-tenant SaaS?

---

## 2. Scope of MVP (15 min)

The data model has 22 tables. We can't build all of them at once. I'm proposing this MVP:

**Phase 1 (must-have for v1.0)**:
1. Auth + RBAC (login, roles, permissions)
2. Property setup (blocks, units)
3. Residents + family members
4. Bills + payments
5. Complaints + maintenance requests

**Phase 2 (after v1.0 ships)**:
6. Facility booking
7. Visitor management
8. Announcements + events
9. Notifications
10. Activity logs / audit

**Decision needed**: Does Anep agree with this split? Anything he wants moved between phases?

---

## 3. Tech stack (10 min)

Already chose: **Spring Boot 3 + Java 17 + Maven + PostgreSQL 16 + React 18 + TypeScript + Vite + Tailwind**.

See `02_STACK_RATIONALE.md` for the full "why". Talking points:

- **Spring Boot, not Laravel**: I know Laravel from CREAMS but Spring Boot is enterprise-grade, has stronger typing, and the JPA/Hibernate stack handles the 22-table model with rich relationships better.
- **React, not Vue/Angular**: Because **after web ships, we port to mobile via React Native** — the auth flow, API client, types, and hooks layer come with us.
- **PostgreSQL, not MySQL**: Row-Level Security is built-in. For multi-tenant this is non-negotiable — even a SQL injection cannot leak data across condos.
- **Monorepo (backend + frontend in one repo)**: Schema, API, and client stay in lockstep. Industry standard for this size.

**Decision needed**: Anep aligned? Any preferences I should know?

---

## 4. Collaboration model (15 min) — biggest source of friction if unclear

These need explicit answers tonight, not assumptions:

| Question | My proposal | His call |
|---|---|---|
| **Who does what** | I lead backend (Spring Boot, schema, auth). He leads frontend (React UI). Both review each other's PRs. |  |
| **Repo location** | `github.com/FaisalHanafi98/MyComunity` (private) — or shared org if he wants joint ownership |  |
| **Branching** | `main` protected, `dev` for integration, `feature/*` for work |  |
| **Code review** | Every change goes through PR; min 1 approval before merge |  |
| **Communication channel** | WhatsApp for quick stuff, GitHub Issues for tracked work |  |
| **Meeting cadence** | Weekly 30-min sync (e.g. Sunday evening) |  |
| **Decision authority** | Stack/architecture: I propose, both agree. Product/scope: he proposes (he knows the domain better?), both agree. |  |
| **Naming the project** | "MyComunity" or rename? PDF spelling has one 'm'. Keep it or fix to "MyCommunity"? |  |

---

## 5. Timeline & commitment (10 min)

Be honest with him AND yourself. Don't over-promise.

**Realistic**:
- I can put in ~6 hours/week (evenings + weekend mornings) given my Accenture job
- His availability: ASK HIM
- Phase 0–7 of the plan (env, scaffold, schema, auth, frontend, CI/CD): **4–6 weekends**
- First working demo (login + create resident + create complaint): **6–8 weeks**
- v1.0 MVP: **3–4 months**

**Decision needed**: Does this timeline work for him? What does he expect?

---

## 6. Open questions to resolve tonight (5 min)

Hand him this list. Don't try to answer them all — just identify which are blockers.

1. ✅/❌ Multi-tenant from day 1?
2. ✅/❌ MVP scope (5 modules above)?
3. ✅/❌ Stack OK?
4. ✅/❌ Repo under `FaisalHanafi98` or shared org?
5. Project name: `MyComunity` or `MyCommunity`?
6. Billing currency: MYR only or multi-currency?
7. Languages: English only, or English + Bahasa Malaysia?
8. Deployment target: Where should v1.0 live? (Railway, Render, Fly.io, or VPS — can defer)

---

## 7. After the meeting — concrete next steps

If he agrees to everything, **next 48 hours**:
1. I install dev environment (Java 17, Maven, Node, Docker, gh)
2. Rename `Anep's Project` → `MyComunity` (or `MyCommunity`)
3. Create local git repo (no GitHub push yet — wait for his confirmation on repo location)
4. Scaffold Spring Boot backend + Vite React frontend
5. Send him the first commit for review

If he disagrees on something major (e.g. wants Laravel, wants single-tenant), pause and re-plan together.

---

## 8. Things to NOT say tonight

- Don't dive into Hibernate annotations or React hooks. He doesn't need to hear it. Save that for code review.
- Don't say "I already wrote all the migrations." (You haven't.) Just say "the schema is mapped out."
- Don't promise dates you can't hit. Add 30% buffer to everything.
- Don't over-sell. Be a partner, not a vendor.

---

## 9. Things to LISTEN for

- Does he have a strong preference you haven't heard before? (e.g. "I really wanted to learn Vue")
- Does he have a deadline you don't know about? (e.g. final year project, resume need)
- Is there a third party expecting this? (his condo MC? a friend who runs property management?)
- What does success look like for HIM? (Portfolio piece? Real product? Learning experience?)

These shape everything.

---

## 10. Closing line (1 min)

> "Cool. So my action items are: install the env, scaffold the project, send you the first commit by [DAY]. Your action items are: [whatever he commits to]. Same time next week?"

Walk away with **his action items written down**. If there are none, that's a red flag.
