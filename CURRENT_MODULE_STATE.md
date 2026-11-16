# CURRENT_MODULE_STATE

## P07 and P08 frontend — 13 November 2026

The authentication shell and property workspace passed the local frontend and
browser checks. See the [current checkpoint](docs/ai-context/01_CURRENT_STATUS.md#p07-shell-and-p08-property-screens-verified--13-november-2026).

## P08 property backend — 12 November 2026

The property APIs passed native PostgreSQL verification on `igumi`. P08 Admin
screens and the J1 browser checks depend on P07. See
[the current checkpoint](docs/ai-context/01_CURRENT_STATUS.md#p08-property-backend-verified--12-november-2026).

Current checkpoint — 29 October 2026: supported Spring runtime verification
passes 111 native tests, including V30's bounded global owner policies. Harness,
mapping, permission-link, V29 and complaint-history
repairs are committed; P03/P04 acceptance has explicit remaining gaps. P05,
production platform provisioning, full G-RLS, frontend and hosted CI are unfinished.
Docker, hosting/VPS and deployment remain deferred. The isolated
[supported-runtime upgrade](docs/adr/0002-supported-spring-baseline.md) passes
local verification. The explicit global SQL policy is also verified; complete
the remaining constraint/enum and privileged mapping cases before P05.
See [current status](docs/ai-context/01_CURRENT_STATUS.md) and the
[native run guide](docs/testing/NATIVE_POSTGRES.md) for evidence and limits.
The owner authorises continuing checked local commits with SOP/Humanizer messages;
keep PRs meaningful and obtain explicit authorisation before any push.

The entries below are historical snapshots.

Current execution revision — 27 October 2026, after readiness planning:
native PostgreSQL 16 and GitHub verification are the current path. Docker,
Testcontainers parity, packaging, hosting/VPS, CD and hosted operations are
deferred until later P18 after local acceptance. The first work is F0 native
test-harness adaptation (HealthIT currently starts a container), safe owned
test-target/role verification and missing foundation tests. Full I1–I9 local
G-RLS still precedes local feature work; activated CI repeats them before merge.
No runtime verification or application/SQL changes occurred in this plan revision.
Use [the revised sequence](docs/plans/IMPLEMENTATION_READINESS_2026-10-27.md#delivery-sequence-after-the-owner-revision)
and [current status](docs/ai-context/01_CURRENT_STATUS.md). Older Docker-gated
next actions below are historical and are superseded by this section.

Planning continuation — 27 October 2026: [actor-driven readiness plan](docs/plans/IMPLEMENTATION_READINESS_2026-10-27.md)
and [rendered consolidated ERDs](docs/database/consolidated-erd/README.md) are
available. Offline test-compile passed today; no tests ran. Docker's Linux
engine was unavailable at that check. F0/runtime/P05/G-RLS remain open; no feature code or SQL
was changed by planning.

Review update — 26 October 2026: [Anep ERD improvement and consolidated-version
brief](docs/plans/ANEP_ERD_CONSOLIDATION_2026-10-26.md) is available. This is a
proposal only; F0/P05/G-RLS implementation status is unchanged.

## Resume pointer — 22 October 2026

File organization is complete; use [docs/README.md](docs/README.md) for the new
layout and [archive/README.md](archive/README.md) for superseded sources.
The review pack now belongs to `docs/research/stakeholder-review-2026-10-22/`.

Current evidence is in
[`docs/ai-context/01_CURRENT_STATUS.md`](docs/ai-context/01_CURRENT_STATUS.md),
section "Current evidence and Gemini consolidation — 22 October 2026".
P03/P04 are partially authored against their packet DoD; earlier test-compile
passed, tests were not executed, Docker's Linux engine remains unavailable,
and P05/G-RLS is unproven. Gemini reconciliation is complete with a bounded
[F0 work order](docs/plans/STAKEHOLDER_RECONCILIATION_2026-10-22.md).
User confirmed landed RA first, hybrid as RA plus commerce, and budget undecided.
Application implementation has not started in the consolidation task.
The historical module block below contains stale P02/P03 and rename statements.

> Living engineering state file. Update at every module boundary and at every session end.
> Last updated: 08 July 2026 (P02 committed at 794a641 — compile-verified; Docker-gated DoD open)

```
Current Epic:      E1 Foundation (blueprint 07_IMPLEMENTATION_ROADMAP §1)
Current Module:    P04 Core domain entities and JPA mappings — COMMITTED (9e6e22d)
Next Module:       P05 Security & Tenancy wiring (JWT filter + TenantContext + @TenantId resolver)
Status:            P04 entities and Spring Data repositories complete across all 26
                   relations. Compile-verified and Spotless clean (55 Java classes).

Completed:
- P02 backend skeleton authored and compile-verified.
- P03 database migrations V1–V28 authored and committed (4e7e257, 807e560, f82927c).
- P04 core JPA domain entities and Spring Data repositories authored across all 11 domain
  packages (TenantAwareBaseEntity, Tenant, SuperAdmin, User, Role, Permission, Block, Unit,
  Resident, FamilyMember, PaymentMethod, Bill, Payment, Complaint, ComplaintCategory,
  ComplaintUpdate, MaintenanceRequest, Facility, FacilityBooking, Announcement, Event,
  Visitor, VisitorPreRegistration, Notification, ActivityLog, RefreshToken).
- Fixed mvnw.cmd to safely handle single quotes in directory paths.
- Verified: mvn spotless:apply test-compile = BUILD SUCCESS (55 classes compiled).

Still open (Docker-gated, P02 DoD):
- U1 ./mvnw verify (HealthIT on Testcontainers PG16) — needs Docker daemon
- U2 ./mvnw spring-boot:run against compose DB + current_user=baiti_app log proof
- U3 compose role checks (also closes P01 residue)
- U4 full verify incl. JaCoCo report
- Docker daemon currently unavailable (installation problem; user deferred)

Carried over (unchanged from prior session):
- Physical folder rename to `baiti` (locked by any live session; run when none active:
  Rename-Item "C:\Users\asbou\Desktop\Work\Development\Anep's Project (Baiti)" -NewName "baiti")
- G-PUSH: remote target decided (FaisalHanafi98/baiti private); creation/push needs explicit instruction.

Blocked:
- P02 full DoD blocked only by the Docker daemon.
- P03 must not start until P02 DoD is fully green (HealthIT + boot proof evidenced).

Regression Risk:   None in production sense (no deployed code). Compile-verified only; runtime
                   behaviour unproven until Docker is back.

Master Plan:       docs/plans/EXECUTION_PLAN_v1.md (complete externalised execution plan)

Next Action:       1) Fix Docker (user). 2) Run U1–U4 (docs/audits/PHASE1_BACKEND_BOOTSTRAP_EVIDENCE.md
                   §5), append results to the evidence file. 3) Commit the evidence refresh + this
                   state file. 4) Start P03 (PACKET_03).

Quality Gate:      P02 DoD PARTIALLY met — compile + Spotless GREEN; HealthIT + boot proof PENDING
                   Docker. G-RLS not applicable yet (arrives with P05).
```
