# P16 — Visitors Module (v1.x)

**Objective**: Guardhouse operations: resident pre-registration with single-use codes, gate check-in/out, live on-site list — journey J4.

**Scope**: pre-registrations (FR-VIS-01/04); code verification + check-in, walk-ins, exit stamping, on-site list (FR-VIS-02/03); Guard console UI + resident pre-reg screens. Out: QR codes, badge printing, blocklists.

**Dependencies**: P18 epic gate — technically P09. **Requirements**: FR-VIS-01…04 · matrix rows `VISITOR_*`.

**Files** — backend `my.baiti.visitors/`: `PreRegistrationService` (+ code generator), `PreRegistrationController` (`/api/visitor-pre-registrations`), `VisitorService`, `VisitorController` (`/api/visitors`, `POST /api/visitors/check-in` {approvalCode|walk-in payload}, `POST /api/visitors/{id}/check-out`, `GET /api/visitors/on-site`), DTOs, `VisitorModuleIT`. Frontend: `pages/visitors/{PreRegisterPage,MyVisitorsPage,GuardConsolePage(on-site list + code entry),WalkInForm}.tsx` + hooks/specs.

**Architecture deltas**: approval code = 8-char unambiguous alphabet (no 0/O/1/I), unique per tenant (04 §4.7), generated server-side, shown to the resident once; verification path: code valid ∧ status PENDING ∧ visit_date = today ⇒ create `visitors` row (entry_time now, `pre_registration_id` linked) + mark pre-reg USED — one transaction; wrong-date/used/unknown ⇒ 404-shaped generic failure (no code oracle). Nightly expiry marks stale PENDING → EXPIRED (simple scheduled job — the pattern P17 generalises). Guard reads are slim projections (name + unit; SRS §8.5). Check-out sets exit_time with the 04 §4.7 CHECK sanity.

**Testing**: README §3 set + single-use proof (second check-in with same code fails) + date-window proof + on-site list correctness (partial index path) + Guard-projection field-level assertion (no phone/email in payload) + code non-enumerability (generic failures).

**DoD**: README §3 module DoD + J4 browser evidence from both sides (resident pre-registers → guard checks in/out).
