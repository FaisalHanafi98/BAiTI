# P08 — Property Module (blocks, units)

**Objective**: First domain module end-to-end; deliberately the simplest — it proves the module pattern (README §3) before the heavyweights.

**Scope**
- In: blocks CRUD, units CRUD, delete-restriction rules, occupancy status field (manual set v1.0; auto-hook lands in P09), Admin screens.
- Out: residents (P09); bulk unit import (backlog).

**Dependencies**: backend P05 · frontend P07. **Requirements**: FR-PROP-01…04 · matrix rows `BLOCK_MANAGE`, `UNIT_MANAGE`, `UNIT_READ_ALL`.

**Files to create** — backend `my.baiti.property/`: `BlockService`, `UnitService`, `BlockController` (`/api/blocks`), `UnitController` (`/api/units`), dto records ×6; tests `PropertyModuleIT`. Frontend: `pages/property/{BlocksPage,UnitsPage}.tsx`, `components/property/{BlockForm,UnitForm,UnitsTable}.tsx`, `hooks/useBlocks.ts`, `useUnits.ts` + Vitest specs.

**Expected architecture** (deltas from README §3 pattern)
- `DELETE /api/blocks/{id}` with units ⇒ 409 problem+json (FR-PROP-03); same for units with residents (guard exists now, becomes reachable in P09).
- Unit list: filter by block, status; sorted by block/unit_no; `UNIQUE(tenant_id, block_id, unit_no)` violation surfaces as 409 with field error.
- Journey J1 steps 2–3 are the UI acceptance script.

**Testing requirements**: README §3 module set — including the cross-tenant probe (tenant A queries B's block id ⇒ 404) and matrix rows (Manager: read-only; Resident/Guard: as per grid; Guard sees unit numbers only via the read endpoint's slim projection).

**Definition of Done**: README §3 module DoD + J1 (blocks/units portion) browser evidence.
