# P09 — People Module (user admin, residents, family members)

**Objective**: The tenant's human graph: staff/resident user accounts, resident registration bound to units, family members, move-out lifecycle, and the automatic unit-occupancy hook.

**Scope**
- In: user management (FR-RBAC-05), residents CRUD + move-out (FR-RES-01…04), family members (FR-RES-03), occupancy auto-status (FR-PROP-04), role-assignment editing screens.
- Out: password reset by email (FR-AUTH-07, v1.x with the email channel); bulk import.

**Dependencies**: P08. **Requirements**: FR-RBAC-01/02/05, FR-RES-01…04 · matrix rows `USER_MANAGE`, `ROLE_MANAGE`, `RESIDENT_MANAGE`, `RESIDENT_READ_ALL`, `FAMILY_MANAGE_OWN`.

**Files to create** — backend `my.baiti.people/`: `UserAdminService`, `UserAdminController` (`/api/users`), `RoleController` (`/api/roles` — list + permission-assignment editing within tenant, `is_system` roles undeletable), `ResidentService`, `ResidentController` (`/api/residents`), `FamilyMemberController` (nested `/api/residents/{id}/family-members`), DTOs; tests `PeopleModuleIT`, `OwnScopeIT`. Frontend: `pages/people/{UsersPage,RolesPage,ResidentsPage,ResidentDetailPage}.tsx`, forms/tables/hooks + specs.

**Expected architecture** (deltas)
- Creating a user: Admin supplies name/email/role; temp password generated, `must_change_password=true`; response returns the temp password exactly once (never logged). Deactivation revokes refresh-token family (calls P05 `RefreshTokenService`).
- Resident creation may link an existing user or none (04 §4.4 nullable `user_id`); linking enforces the one-active-residency partial unique — violation ⇒ 409 with a domain message.
- Move-out (FR-RES-04): status → `MOVED_OUT`, linked portal access decision (deactivate user if their only residency), occupancy hook re-evaluates unit → `VACANT` when no active residents remain; assign hook sets `OCCUPIED` (FR-PROP-04 lands here where residents exist).
- Family members: `FAMILY_MANAGE_OWN` means a Resident manages only their own resident record's family — own-scope predicate via `ResidentScopeGuard` (P05c).
- Events: `USER_CREATED/DEACTIVATED`, `RESIDENT_MOVED_OUT` → audit (always) — no notifications in this module.

**Testing requirements**: README §3 set, plus: temp-password flow (login forces change); deactivated user's refresh rejected; one-active-residency conflict; occupancy transitions both ways; own-scope proof (Resident A cannot touch Resident B's family within the same tenant); `is_system` role deletion ⇒ 409.

**Definition of Done**: README §3 module DoD + journey J1 completed end-to-end in browser (Admin builds property, staff, first resident; that resident logs in and sees their unit) with evidence filed.
