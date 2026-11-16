# BAiTI project overview

**Status as of 16 November 2026**

BAiTI is a multi-tenant web platform for Malaysian neighbourhoods. Each community has its own accounts, permissions and records. The first pilot is a landed residents’ association (RA). The confirmed product direction combines RA management with later neighbourhood commerce. Platform operators onboard communities, authorised committee members run association operations, and residents access their own household and community services. A Guard role supports limited operational views where the community has an actual guard service. [Owner decisions](research/stakeholder-review-2026-10-22/consolidation/OWNER_DECISIONS.json) · [Product reconciliation](plans/STAKEHOLDER_RECONCILIATION_2026-10-22.md)

## What works today

| Area | Implemented behavior | Availability |
|---|---|---|
| Platform onboarding | A platform operator signs in separately, lists communities and creates a community. Creation commits the community, roles, permissions, first administrator and audit record together. | Published on GitHub |
| Tenant authentication and access | Community sign-in, first-use password replacement, identity lookup, rotating refresh credentials and sign-out. The backend enforces named permissions and checks resident ownership where applicable. The four default tenant roles are Admin, Manager, Resident and Guard. | Published on GitHub |
| Tenant isolation | Signed tenant claims, tenant-scoped queries and PostgreSQL row-level security isolate records. Flyway and the runtime use separate roles. V32 enforces tenant matching on 28 domain foreign keys, and V33 checks existing references. | Published on GitHub |
| Browser workspace | Sign-in, password replacement, a protected overview, session recovery and sign-out. Requests within a tab share one refresh. Browsers with Web Locks coordinate refresh-cookie changes across tabs. | Implemented and verified locally |
| Property | Admins can create, view, edit and delete blocks and units. Unit lists support pagination, filters and manual occupancy. Managers can read unit details. Guards see unit numbers only. The API handles duplicate names or numbers, prevents deletion of referenced records, and returns the same neutral 404 for missing and foreign records. When a write response is uncertain, the editor preserves the draft and requires a successful reload before another attempt. | Implemented and verified locally |

The application is a modular Spring Boot monolith with Java 17 source, a verified Java 21 runtime, Maven, PostgreSQL 16 and Flyway. The local web client uses React 18, TypeScript and Vite with client types generated from OpenAPI. [Architecture](blueprint/03_ARCHITECTURE.md) · [Schema and migrations](blueprint/04_DATABASE_DESIGN.md)

On GitHub, `main`, `igumi` and `anep` all pointed to `d5ed66d` on 16 November. That revision includes the security foundation and two synthetic development communities. The P07/P08 work and workflow clarifications are uncommitted in the unified BAiTI checkout on `igumi`. The latest recorded local checks passed **330 backend tests**, including tenant isolation and the permission matrix, plus **eight frontend tests**, lint, type checking, a production build and a Chrome journey. The browser run covered first-use login, property creation, occupancy editing, cross-tenant denial, refresh and logout. Hosted CI and deployment remain pending. [Published revision](https://github.com/FaisalHanafi98/BAiTI/commit/d5ed66daac5af41f1f9869dd23d443748ae65c8e) · [Local verification](ai-context/06_TESTING_EVIDENCE/2026-11-13_p07_p08_frontend.md)

## Accepted design and planned delivery

- **Development and release.** Use native PostgreSQL 16 and isolated local test databases. GitHub CI should repeat the migration, tenant-isolation and authorisation checks before merge. Docker/Testcontainers, hosting, continuous deployment and real pilot traffic are deferred. The hosting budget is undecided. [Accepted delivery sequence](plans/IMPLEMENTATION_READINESS_2026-10-27.md#delivery-sequence-after-the-owner-revision)
- **P09 People is next.** Plan staff and resident accounts, role assignment, deactivation, resident and family records, and move-out history. Active residency should automatically determine whether a unit is occupied. The handling of already-issued access tokens after deactivation remains open. [P09 work order](blueprint/08_ai_packets/PACKET_09_people.md)
- **Core RA operations.** The blueprint plans MYR billing, manual payment recording and balances, complaints with a visible history, maintenance, and later reporting and operational hardening. Current workflow rules allow cancellation of an unpaid bill with a reason, resident close or reopen of their own resolved complaint, and staff rejection of a pending maintenance request. [Roadmap](blueprint/07_IMPLEMENTATION_ROADMAP.md) · [State transitions](blueprint/STATE_TRANSITIONS.md)
- **Later blueprint modules.** Facilities and booking, announcements and events, visitor pre-registration and check-in, and the notification centre follow the core modules. The booker can cancel a pending or approved booking before the slot starts. Visitor check-in needs an atomic, single-use transition that keeps visit status and timestamps consistent. [Work packet index](blueprint/08_ai_packets/README.md) · [State transitions](blueprint/STATE_TRANSITIONS.md)

## Proposed extensions and open decisions

| Proposal | Decision needed |
|---|---|
| Landed address verification and targeted notices | Confirm the pilot RA, its governing rules, acceptable evidence, reviewers, appeals and retention. Treat residency verification, RA membership, billing liability and committee permissions as separate decisions. |
| Scheduled dues, payment integrations, reminders and financial transparency | Agree who owes fees and from when. Confirm merchant ownership and settlement, provider selection, messaging consent and which financial details residents may see. DuitNow QR, FPX and WhatsApp are candidates. |
| Neighbourhood commerce | Assess local services, food preorders, permitted pickup points and neighbour endorsements. Resolve seller onboarding, cancellations, refunds and moderation. Direct seller settlement is a design proposal. |
| Safety and monetisation | Give SOS and patrol separate operating approvals and drills. Subscription prices and fees remain open. Carpool, paid placement and advertising are deferred. |

The consolidated ERDs show how these proposals could extend the existing model. Schema changes are governed by the database blueprint and tracked migrations. [Consolidated ERDs](database/consolidated-erd/README.md) · [Open decision worksheet](plans/STAKEHOLDER_RECONCILIATION_2026-10-22.md#10-business-decision-worksheet-and-remaining-unknowns)
