# Authentication activity audit — 8 November 2026

The first native `AuthAuditIT` run signed in to both synthetic communities
and found no activity rows. The implementation captures the originating
tenant, actor, action, request address and occurrence time before handing
the event to a single asynchronous worker. The worker inserts through
`baiti_app` and clears its tenant context before returning to the queue.

The test checks two successful sign-ins and one rejected password.
Each tenant can see its own events and neither can see the other's. A
task submitted after the writes checks the worker context is empty.
Fixture teardown removes the test-owned rows by tenant and request address.

The complete native run passed 151 unit/architecture and 162 integration
tests with zero failures, errors or skips. Raw output is in the ignored
`docs/audits/auth-audit-2026-11-08/` directory. No migration changed.

The executor queues up to 128 waiting events. Queue rejection and failed
inserts are logged and the authentication response can still succeed.
The queue is in-process, so an abrupt shutdown can lose pending events.
This verifies the I8 mechanism. The combined I1–I9 suite and dev-profile
journey still require evidence.
