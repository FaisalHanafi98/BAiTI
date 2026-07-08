# P12 — Maintenance Module (requests & queue)

**Objective**: Work-order flow reusing P11's state-machine template: raise → approve → do → complete, with a priority-aware staff queue.

**Scope**
- In: requests CRUD-lite (FR-MNT-01), lifecycle (FR-MNT-02), staff queue with filters (FR-MNT-03), notifications on transitions.
- Out: assignment-to-contractor, scheduling, cost tracking (all backlog).

**Dependencies**: P09 (and P11 first is strongly recommended — copy its guard pattern). **Requirements**: FR-MNT-01…03 · matrix rows `MAINTENANCE_CREATE_OWN/READ_OWN/READ_ALL/PROGRESS`.

**Files to create** — backend `my.baiti.maintenance/`: `MaintenanceService` + state guard, `MaintenanceController` (`/api/maintenance-requests`, `POST …/{id}/transition`), DTOs; tests `MaintenanceModuleIT`, guard unit test. Frontend: `pages/maintenance/{MyRequestsPage,RequestQueuePage,RequestDetailPage}.tsx` + form with priority select, hooks, specs.

**Expected architecture** (deltas)
- States `PENDING → APPROVED → IN_PROGRESS → COMPLETED`, `PENDING → REJECTED` (02 §4.4 note); rejection message mandatory.
- Resident raises against own unit (auto-derived residency, as P11); staff on-behalf allowed.
- Queue: filter status/priority/block, default sort priority desc then age; pagination standard.
- Transitions notify the requester; creation notifies Managers. Audit on all transitions.

**Testing requirements**: README §3 set + transition matrix + queue filter/sort correctness + own-scope proof.

**Definition of Done**: README §3 module DoD; browser evidence of raise→approve→complete round-trip with notifications visible.
