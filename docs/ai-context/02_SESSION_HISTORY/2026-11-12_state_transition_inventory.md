# State transition inventory — 12 November 2026

Audited the live request and service paths, entity fields, database defaults
and constraints, native tests, and planned workflow packets. Recorded the
application-owned transitions, derived gates and recovery rules in the
[state transition inventory](../../blueprint/STATE_TRANSITIONS.md).

Use the existing domain service or pure guard as the authority for each
transition. For changes to workflow behavior, link the action, guard, result,
side effects and recovery to the relevant route and tests. The P07 packet now
points to the sign-in and refresh guidance. The inventory records conflicts
for the packets that own the planned workflows.

The changes are documentation only. The [verification record](../06_TESTING_EVIDENCE/2026-11-12_state_transition_inventory.md)
records link checks, backend source and test fingerprint parity, and the
330-test native run after starting the existing local PostgreSQL cluster.
