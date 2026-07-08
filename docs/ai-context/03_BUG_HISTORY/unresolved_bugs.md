# Unresolved Bugs

> **Last Updated**: 2026-05-10
> **Project Phase**: Pre-code (planning only)

---

No bugs are currently tracked. No code exists yet. This file will be populated once implementation begins.

## Known Pre-Implementation Risks (Not Bugs Yet — Watch For These)

The following are not bugs but are risks identified in the plan that could manifest as bugs if implementation isn't careful:

| Risk ID | Description | Watch-For Trigger | Acceptance Criteria |
|---|---|---|---|
| R11 | Cross-tenant data leak | Any query that doesn't filter by `tenant_id` at DB layer | `tenant_isolation` RLS policy on every domain table; cross-tenant test green |
| R12 | Forgotten `tenant_id` in native/raw SQL | Any `@NativeQuery` or `entityManager.createNativeQuery()` call | RLS catches at DB; code review rejects native queries without `tenant_id` |
| R13 | Partial tenant onboarding state | `POST /admin/tenants` fails mid-transaction | Full `@Transactional` boundary; idempotency on tenant slug |
| R6 | JWT refresh rotation locks users out | Refresh token rotation race condition | Extensive security tests; feature flag for rotation disable |

---

## Template for Future Entries

```
## BUG-{n}: {Short title}

- **Reported**: {date}
- **Phase**: {SDLC phase when found}
- **Symptom**: {what is observed}
- **Evidence**: {log lines, test failures, browser behavior}
- **Likely Cause**: {best current hypothesis}
- **Next Checks**: {what to look at next}
- **Files Likely Involved**: {list}
- **Acceptance Criteria**: {how to confirm fixed}
- **Attempts Made**: {list of failed fixes — link to failed_attempts.md}
```
