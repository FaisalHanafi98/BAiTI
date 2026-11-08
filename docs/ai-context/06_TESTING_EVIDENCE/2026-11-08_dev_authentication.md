# Development profile authentication — 8 November 2026

The original dev build started on the separate owned
`baiti_dev_v32_20261108_bfc13f4b` database with seeding requested and found
zero tenant rows. With the seed runner in place, a mismatched database marker
aborted startup without changing the tenant count. The correct marker created
Rumbia Gardens and Kenari Grove through platform onboarding.

Both communities completed the loopback HTTP checks with curl.

| Check | Rumbia Gardens | Kenari Grove |
|---|---|---|
| First sign-in and identity check | 200 / 200 | 200 / 200 |
| Replace the initial password | 204 | 204 |
| Sign in and fetch identity with new credentials | 200 / 200 | 200 / 200 |
| Use the token with the other community slug | 401 | 401 |
| Refresh with allowed origin and explicit refresh header | 200, rotated cookie | 200, rotated cookie |
| Log out, then retry the rotated refresh credential | 204 / 401 | 204 / 401 |
| Restart with seeding enabled | Same tenant ID and changed password | Same tenant ID and changed password |

The first run reached the default shared loopback rate limit on Kenari's
sign-in after both initial passwords had already changed. The next run
completed the remaining journey at the same default rate limit. After restart
both original passwords returned 401 and the replacement passwords returned
200. The smoke runner stopped its application process after the checks.

Raw logs are ignored under `docs/audits/dev-seed-2026-11-08/`. The dev settings
and current synthetic passwords are kept in private ignored files under
`backend/.local/postgres16/`. The native suite separately passed 321 tests.
JaCoCo covered 1,023 of 1,121 executable Java lines changed since the P04
baseline `c4ffbac` (91.3%). This was a local comparison with the native run's
report.
