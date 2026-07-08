# P14 — Facilities Module (v1.x)

**Objective**: Facility catalogue + race-safe slot booking with approval workflow.

**Scope**: facilities CRUD (FR-FAC-01); bookings create/cancel/list (FR-FAC-02); approval flow (FR-FAC-03); resident + staff screens. Out: recurring bookings, deposits/charges, calendars beyond a simple day view.

**Dependencies**: P18 (v1.x epic) — technically only P09. **Requirements**: FR-FAC-01…03 · matrix rows `FACILITY_*`, `BOOKING_*`.

**Files** — backend `my.baiti.facilities/`: `FacilityController` (`/api/facilities`), `BookingService`, `BookingController` (`/api/facility-bookings`, `POST …/{id}/approval`), state guard, DTOs, `FacilityModuleIT` + `BookingRaceIT`. Frontend: `pages/facilities/{FacilitiesPage,BookFacilityPage,MyBookingsPage,BookingApprovalsPage}.tsx` + hooks/specs.

**Architecture deltas**: slot double-booking is settled by the partial unique index (04 §4.7) — the service catches the constraint violation and answers 409 `SLOT_TAKEN`; **no** pre-check-then-insert as the only guard. States per 02 §4.5; cancel by booker only before slot start; approval records `approved_by`. Notifications: decision → booker. Facility status `MAINTENANCE/CLOSED` blocks new bookings (409) but not history.

**Testing**: README §3 set + `BookingRaceIT` (parallel same-slot bookings ⇒ exactly one PENDING) + state matrix + own-scope (see/cancel own only) + facility-status blocking.

**DoD**: README §3 module DoD + browser evidence of book → approve → conflict-reject round-trip.
