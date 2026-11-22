# P09 user administration — 22 November 2026

Continued implementation on `igumi` from `0f8737d` under the owner's direction
to stage the work and prepare SOP messages for small commits. The three existing
local commits and earlier planning edits were present at intake.

Implemented the bounded account administration slice in
[P09](../../blueprint/08_ai_packets/PACKET_09_people.md). Tenant administrators
can list/create accounts, select a tenant role, assign another role and deactivate
an account. Creation returns a generated password once and enforces the existing
first-use flow. Role changes and deactivation revoke refresh credentials and
record an audit atomically. The read-only role picker permits `USER_MANAGE` or
`ROLE_MANAGE`, including custom account administrators. Account writes require
`USER_MANAGE`. Existing access tokens retain the signed lifetime contract.

The Users route includes translated validation/status feedback, a paginated
directory, confirmations, draft preservation and a block on automatic
resubmission after an unconfirmed write. The temporary password lives in the
component state and is dismissed explicitly. It is excluded from query/mutation
cache results and browser storage.

[Backend evidence](../06_TESTING_EVIDENCE/2026-11-22_people_accounts.md) records
151 unit/architecture and 190 integration tests, the operation-ID regression
and 97.6% changed-line coverage.
[Frontend evidence](../06_TESTING_EVIDENCE/2026-11-22_people_users_ui.md) records
14 tests, static/build checks and the real Chrome journey. The generated API
artifact preserves all previous operations and schemas. Applied migrations
retain their existing bytes.

At the staging checkpoint, the review queue in `.git/baiti-staging/2026-11-22/` contained two ordered patches,
complete SOP messages and an explicit path manifest. The first group contains
the People API, frozen contract, tests and backend evidence. The second contains
the Users interface, its tests and session handoff. Both groups were staged in
the real index. An alternate index verified that the patches applied in sequence
from the recorded base and reproduced that staged tree.

The owner then authorised committing and publishing to GitHub. The account API
was committed as `79caf75` with its SOP message. The following interface commit
includes this updated handoff and its own SOP message. Publication targets the
existing `origin/igumi` branch, including the three earlier local commits that
were absent from that remote branch at the fetch. Remote delivery is checked by
comparing the advertised branch SHA with local HEAD. Earlier unrelated planning
edits remain unstaged.

Continue P09 with resident registration and unit binding, occupancy updates,
family ownership, move-out and role permission editing. J1 completion still
depends on the resident login and owned-unit journey. P06 hosted verification
remains an open merge gate.
