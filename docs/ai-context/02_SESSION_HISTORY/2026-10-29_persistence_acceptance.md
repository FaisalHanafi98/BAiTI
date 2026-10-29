# Persistence acceptance checkpoint — 29 October 2026

Completed the bounded P03/P04 acceptance continuation after V30. New tests cover
all authored unique indexes and enum fields, global JPA mapping, timestamp
persistence, invalid checks and the lazy-association contract. A catalog regression
exposed six unindexed FK relationships. Forward V31 adds those six indexes;
applied V1–V30 were not edited.

Both an upgraded V30 target and a separate fresh V1–V31 target passed 159 tests
(59 unit/architecture + 100 integration, no failures/errors/skips). Fresh migration
was tested before the named fixtures were explicitly inserted through the
ordinary owner. Source, log hashes and scope limits are in
[the acceptance record](../../audits/persistence-acceptance-2026-10-29/P04_ACCEPTANCE.json).

This supplies local persistence prerequisite evidence for P05. Next is slice 5a,
tenant context propagation and safe pool return; JWT, RBAC, platform onboarding,
full I1–I9 and changed-line coverage remain open. Docker/hosting remain deferred.
Local commits follow the owner's SOP/Humanizer instruction; no push or PR.
