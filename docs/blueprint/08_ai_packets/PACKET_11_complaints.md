# P11 — Complaints Module (categories, complaints, update trail)

**Objective**: The resident-facing workflow flagship: submit → triage → resolve → close, with a complete visible trail and notification fan-out. First full state-machine module — its transition-guard pattern is the template P12/P14 reuse.

**Scope**
- In: categories CRUD (FR-CMP-05), submission (FR-CMP-01), lifecycle + updates trail (FR-CMP-02/03), notifications (FR-CMP-04), resident + staff screens.
- Out: photo attachments on complaints (backlog — StorageService exists; keep the door open in DTO design), SLA timers.

**Dependencies**: P09 (residents). **Requirements**: FR-CMP-01…05 · matrix rows `COMPLAINT_CREATE_OWN`, `COMPLAINT_READ_OWN/ALL`, `COMPLAINT_PROGRESS`, `COMPLAINT_CATEGORY_MANAGE`.

**Files to create** — backend `my.baiti.complaints/`: `ComplaintCategoryController` (`/api/complaint-categories`), `ComplaintService` (+ `ComplaintStateMachine` guard class), `ComplaintController` (`/api/complaints`, `POST /api/complaints/{id}/updates`), DTOs; tests `ComplaintModuleIT`, `ComplaintStateMachineTest` (pure unit). Frontend: `pages/complaints/{MyComplaintsPage,SubmitComplaintPage,ComplaintQueuePage,ComplaintDetailPage(trail timeline)}.tsx`, components/hooks + specs.

**Expected architecture** (deltas)
- State machine per 02 §4.4 exactly (OPEN/IN_PROGRESS/RESOLVED/CLOSED/REJECTED + resident reopen RESOLVED→IN_PROGRESS and resident close of own RESOLVED). Guard = pure function `(current, target, actorRole, isOwner) → allowed`; unit-tested exhaustively (whole matrix), service consults it; illegal ⇒ 409.
- Every transition = one `complaint_updates` row (updated_by, message mandatory on REJECTED and RESOLVED, resulting status) in the same transaction.
- Submission auto-derives resident + unit from the caller's residency (Resident role); staff may submit on behalf with explicit residentId (`COMPLAINT_PROGRESS` holders).
- Notifications (02 §4.4): OPEN → all Manager-role users; every other transition → the complaint's resident. Via P05 event listeners; tenant travels in the event (I-8 pattern).
- List endpoints: staff queue filterable (status, category, block); resident sees own only; detail includes the full trail ordered asc.

**Testing requirements**: README §3 set, plus: full transition matrix (legal + illegal × roles, driven by the unit-tested guard, spot-checked in slice tests); trail completeness (N transitions ⇒ N update rows); notification fan-out counts per transition; own-scope (resident B can't read resident A's complaint in same tenant ⇒ 404).

**Definition of Done**: README §3 module DoD + journey J3 end-to-end in browser evidenced (submit → notify → progress → resolve → confirm close, trail visible throughout).
