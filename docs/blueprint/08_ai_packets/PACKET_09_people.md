# P09 — People Module (user admin, residents, family members)

**Objective**: The tenant's human graph: staff/resident user accounts, resident registration bound to units, family members, move-out lifecycle, and the automatic unit-occupancy hook.

**Scope**
- In: user management (FR-RBAC-05), residents CRUD + move-out (FR-RES-01…04), family members (FR-RES-03), occupancy auto-status (FR-PROP-04), role-assignment editing screens.
- Out: password reset by email (FR-AUTH-07, v1.x with the email channel); bulk import.

**Dependencies**: P08. **Requirements**: FR-RBAC-01/02/05, FR-RES-01…04 · matrix rows `USER_MANAGE`, `ROLE_MANAGE`, `RESIDENT_MANAGE`, `RESIDENT_READ_ALL`, `FAMILY_MANAGE_OWN`.

## Active user administration slice — 22 November 2026

Owner authorised continuing implementation with staged changes and SOP message
drafts for small commits. This slice delivers FR-RBAC-05 through `/api/users`
and a tenant role picker at `/api/roles`, followed by the Users screen. Preserve
the existing first-use, token expiry and property contracts. Resident/family
workflows and role permission editing follow as separate P09 slices.

The read-only role picker accepts `USER_MANAGE` or `ROLE_MANAGE`, so a custom
account administrator can select a role without permission editing authority.
Account mutations still require `USER_MANAGE`.

The baseline is `0f8737d` on `igumi`, with unrelated planning edits present.
Existing users, roles, RLS and refresh revocation support the slice without DDL.
HTTP validation and method authorities feed `UserAdminService`, existing
repositories and `RefreshTokenService`. Successful writes append a tenant audit
inside the same transaction. Responses expose a generated temporary password
only on creation, with cache prevention and redacted string representations.

Acceptance covers forced password replacement, tenant-scoped paginated reads,
role assignment, deactivation and refresh rejection, duplicate email conflicts,
input validation, denied default non-admin roles and foreign resource 404s.
The client must preserve drafts, prevent duplicate submits after an unconfirmed
write, and keep the temporary password out of query caches and storage.
Use the owned synthetic native PostgreSQL target and fixture-owned cleanup.
Run focused red/green tests, the native regression suite and frontend checks.
Stage explicit paths in backend and frontend groups with their evidence.

**Files to create** — backend `my.baiti.people/`: `UserAdminService`, `UserAdminController` (`/api/users`), `RoleController` (`/api/roles` — list + permission-assignment editing within tenant, `is_system` roles undeletable), `ResidentService`, `ResidentController` (`/api/residents`), `FamilyMemberController` (nested `/api/residents/{id}/family-members`), DTOs; tests `PeopleModuleIT`, `OwnScopeIT`. Frontend: `pages/people/{UsersPage,RolesPage,ResidentsPage,ResidentDetailPage}.tsx`, forms/tables/hooks + specs.

**Expected architecture** (deltas)
- Creating a user: Admin supplies name/email/role; temp password generated, `must_change_password=true`; response returns the temp password exactly once (never logged). Deactivation revokes refresh-token family (calls P05 `RefreshTokenService`).
- Resident creation may link an existing user or none (04 §4.4 nullable `user_id`); linking enforces the one-active-residency partial unique — violation ⇒ 409 with a domain message.
- Move-out (FR-RES-04): status → `MOVED_OUT`, linked portal access decision (deactivate user if their only residency), occupancy hook re-evaluates unit → `VACANT` when no active residents remain; assign hook sets `OCCUPIED` (FR-PROP-04 lands here where residents exist).
- Family members: `FAMILY_MANAGE_OWN` means a Resident manages only their own resident record's family — own-scope predicate via `ResidentScopeGuard` (P05c).
- Events: `USER_CREATED/DEACTIVATED`, `RESIDENT_MOVED_OUT` → audit (always) — no notifications in this module.

**Testing requirements**: README §3 set, plus: temp-password flow (login forces change); deactivated user's refresh rejected; one-active-residency conflict; occupancy transitions both ways; own-scope proof (Resident A cannot touch Resident B's family within the same tenant); `is_system` role deletion ⇒ 409.

**Definition of Done**: README §3 module DoD + journey J1 completed end-to-end in browser (Admin builds property, staff, first resident; that resident logs in and sees their unit) with evidence filed.
