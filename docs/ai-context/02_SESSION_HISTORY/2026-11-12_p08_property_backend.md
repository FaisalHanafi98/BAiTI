# P08 property backend — 12 November 2026

The sibling implementation checkout started clean on `igumi` at
`d5ed66daac5af41f1f9869dd23d443748ae65c8e`, matching the GitHub branch tip.
The P05 native tenant-isolation gate had passed on this workstation.

Implemented block and unit CRUD using the existing tenant-scoped entities and
database constraints. `BLOCK_MANAGE` covers block endpoints, `UNIT_MANAGE`
covers unit writes, and `UNIT_READ_ALL` covers unit reads. Managers receive the
full unit view. Guards receive only unit numbers and can sort by unit number.
Block and occupancy filters require the full unit view. Occupancy is manual
in P08, with automatic residency transitions assigned to P09.

The property services resolve IDs within the request tenant. Existing foreign
keys protect referenced blocks and units, including residency history. Unique
constraints determine duplicate conflicts even when requests race. Successful
writes append the actor, action and property ID to the existing tenant audit
log in the same transaction.

The first application-level test run reproduced missing routes. A relative
path in the initial source write put new files in the planning checkout. The
files were moved to the implementation checkout and Git confirmed that the
planning checkout had no property-source changes. The focused and full suites
then passed. See the [verification record](../06_TESTING_EVIDENCE/2026-11-12_p08_property_backend.md).

The backend changes are in the local working tree. P08 Admin screens and the
J1 browser checks depend on the P07 frontend shell.
