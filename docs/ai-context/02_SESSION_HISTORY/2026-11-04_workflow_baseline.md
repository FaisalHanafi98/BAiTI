# Workflow intake and P05c planning baseline — 4 November 2026

Scope: the user's attached BAiTI project workflow, prepared today. It is a
planning handoff, with no selected product module and no new application/push
authority inferred from its reusable prompt. This session maps the next ready
prerequisite into the existing Packet 05 rather than creating another module plan.

| Resume field | Observed state |
|---|---|
| Current goal | Establish the workflow baseline and bounded P05c acceptance map. |
| Current blocker | Complete P05/G-RLS and relevant RA-domain acceptance are prerequisites for the proposed product modules. |
| Current repo state | Active `backend/.local/github-publication`, branch igumi, HEAD 4ce314b; clean at intake; three local branch refs aligned. |
| Current database state | pg_ctl reports no server running; settings path exists. Target/roles/history not queried. |
| Docs/code alignment | Auth/context source exists; catalogue, method permissions and actor auditing remain incomplete. See the scoped alignment note. |
| Known deviations | Native PG replaces deferred Docker execution; original planning/reorganisation work stays outside the active worktree. |
| Likely valid deviations | Existing tenant-aware auth/refresh should be extended; no replacement auth framework or second frontend. |
| Likely mistakes | Following stale May pre-code navigation or restoring Admin-revoked grants through unconditional startup default sync. The latter is an unimplemented design risk, not a confirmed current runtime defect. |
| Files likely involved | Packet 05, analysis §8, SRS FR-RBAC, SecurityConfig, JpaConfig, people role/grant/authentication source and native tests. Proposed catalogue/sync/guard files are labelled missing in the map. |
| Immediate safe next action | Plan catalogue/default-role parity tests against analysis §8, without DB writes or product expansion. |
| Actions requiring user confirmation | None for this bounded planning inspection. Product policies remain unresolved prerequisites; no external action or schema execution is requested. |

Read active instructions/state, packet protocol, P05, relevant architecture,
access grid, schema deviation register, native testing contract/runner and the
new module prerequisite sections. Read the original stakeholder/readiness plans
without copying dirty source. The original checkout remains detached, with 84
local changes; no checkout was detached, removed, reset or cleaned this session.

Source findings and the next implementation slice are recorded once in
[Packet 05](../../blueprint/08_ai_packets/PACKET_05_security_tenancy.md#planning-baseline-and-acceptance-map--4-november-2026).
Authentication already projects DB role permissions into JWTs. Role/grant
validation exists, but that is not the missing method/own-scope enforcement.
JpaConfig's actor provider remains an explicit empty stub. The full I-suite,
onboarding and async audit are not implemented by this plan.

Observed tools: Java Temurin 21.0.11, Maven 3.9.16. Source migrations count 31,
ending in V31. No frontend directory. The private settings contents were not
read; no environment dump, database connection, test preflight, SQL, migration,
test run or restart occurred. Existing XML/log records were not relabelled as
today's tests. The last full native result is 276 tests on 3 November.

The three proposed extensions remain conditional: procurement needs G-RLS,
actual property/people/maintenance contracts and accepted RA approvals; retrieval
needs document permissions/rights/lifecycle and protected storage; dues/reserve
planning needs actual baseline finance, liability and exact-money contracts.
None changes the landed-RA pilot into statutory strata management.

Outcome: planning documentation only. Author source review used backend,
security and verification guidance; no independent security review is claimed.
No Java/SQL/configuration/infra changes, commits, branches, PRs, pushes, service
starts, installations or deployments were made. Docker/hosting remain deferred.
Verification: six added local links and three heading anchors resolved; whitespace
checks passed. The index remains empty, implementation trees match HEAD and only
the four intended planning documents changed. Local branch refs remain aligned
at 4ce314b and the original checkout still has 84 local changes. These checks do
not establish full G-RLS or production readiness.
