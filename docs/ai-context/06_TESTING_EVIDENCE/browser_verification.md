# Browser Verification Evidence

> **Last Updated**: 2026-05-10
> **Project Phase**: Pre-code (planning only)

---

## Current State

No browser testing has been done. No frontend exists. No backend exists.

---

## Planned Critical Browser Test Flows (for when implementation begins)

These are the flows that MUST be browser-verified before each phase is considered complete:

### Phase 5 (Security + Auth) — Minimum Viable Browser Tests

| Flow | Steps | Pass Criteria |
|---|---|---|
| Login — Happy Path | Navigate to `/login`, enter valid credentials for Tenant A | JWT stored, redirect to dashboard, user name displayed |
| Login — Wrong Password | Enter invalid password | 401 returned, error message shown, no token stored |
| Login — Wrong Tenant | Enter valid credentials but wrong tenant | 401 or tenant resolution error, no cross-tenant access |
| Protected Route — Unauthenticated | Navigate to `/dashboard` without token | Redirect to `/login` |
| Protected Route — Expired Token | Wait for token expiry, access protected route | Auto-refresh OR redirect to login |
| Tenant Isolation — Critical | Login as Tenant A Admin, attempt to access Tenant B resource via API | 404 (RLS hides it), never 403 or 200 |

### Phase 8 (Core CRUD) — Minimum Viable Browser Tests

| Flow | Steps | Pass Criteria |
|---|---|---|
| Create Unit | Admin → Property → Add Unit | Unit appears in list, persisted after refresh |
| Create Resident | Admin → Residents → Add Resident | Resident linked to unit, appears in list |
| Create Bill | Manager → Finance → New Bill | Bill appears in resident's view |
| Pay Bill | Resident → My Bills → Pay | Payment recorded, bill status updated |

---

## Template for Future Entries

```
## Browser Test: {Feature} — {Date}

**Tested By**: {name or "AI agent"}
**Environment**: {dev / staging}
**Browser**: {Chrome/Firefox/Edge}
**Backend Running**: {yes/no, version}

### Flow Tested

| Step | Action | Expected | Actual | Result |
|---|---|---|---|---|

### Issues Found

{description or "None"}

### Screenshots / Evidence

{file references or "None"}
```
