# State transition inventory verification — 12 November 2026

Reviewed the local `igumi` implementation based on `d5ed66d`, including the
P08 backend working tree. Checked each inventory link against the checkout
and compared a SHA-256 manifest of every file under `backend/src` before and
after the audit. The manifest matched.

The native runner's first attempt reached 151 passing unit and architecture
tests, then failed database preflight with SQL state `08001`. The existing
PostgreSQL 16 cluster reported a stopped server. After starting that owned
cluster and checking the loopback listener, the default offline native runner
passed 151 unit and architecture tests plus 179 integration tests. There were
zero failures, errors or skips. Maven exited 0 in 1 minute 23 seconds.

The run includes P08 property behavior, migration validation through V33,
permission checks and I1–I9. See the local
[Failsafe summary](../../../backend/target/failsafe-reports/failsafe-summary.xml).
The frontend shell and browser acceptance remain planned.
