# P08 property backend verification — 12 November 2026

Tested the P08 working tree based on `igumi` at `d5ed66d`. The workstation
used Java 21.0.13, Maven 3.9.16 and the owned PostgreSQL 16.15 test database on
loopback. The native runner retained the separate app and migrator roles.

| Run | Unit and architecture | Native integration | Result |
|---|---:|---:|---|
| Red, `-IntegrationTests PropertyModuleIT` | 151 | 9 | Missing property routes returned 404 |
| Focused, `-IntegrationTests PropertyModuleIT,PermissionMatrixIT` | 151 | 15 | Build success |
| Full, default native verify | 151 | 179 | Build success, zero failures, errors or skips |

The full run included I1–I9, migration validation through V33 and the permission
matrix, which matched all ten property routes to the endpoint fixture and
generated OpenAPI. Property tests covered CRUD, manual occupancy, validation,
page size and sorting, combined filters, neutral 404s, duplicate and reference
409s, Guard projection and audit writes. The tests removed their fixture rows
by their generated identifiers and actor.

The full run exited 0 in 1 minute 11 seconds. JaCoCo covered **188 of 193
changed executable Java lines (97.4%)** using a dedicated execution data file.
See the [Failsafe summary](../../../backend/target/failsafe-reports/failsafe-summary.xml)
and [JaCoCo report](../../../backend/target/site/jacoco/index.html). Frontend
and browser acceptance follow P07.
