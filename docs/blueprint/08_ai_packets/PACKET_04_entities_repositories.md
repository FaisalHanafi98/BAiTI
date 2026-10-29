# P04 — JPA Entities & Repositories

**Objective**: Every table mapped to a JPA entity with the tenant-aware base class, a Spring Data repository per aggregate, repository smoke tests, and the ArchUnit rule set activated.

**Scope**
- In: `TenantAwareBaseEntity`, all entities + enums, repositories, JPA auditing, `@TenantId` wiring (declaration only — the resolver arrives in P05), ArchUnit tests, repo smoke tests.
- Out: services, controllers, DTOs (module packets); TenantContext/resolvers (P05).

**Dependencies**: P03.

**Files to create** (package-by-feature under `my.baiti.…`)
```
common/TenantAwareBaseEntity.java     common/AuditorAwareImpl.java (stub → P05)
common/TenantScopedBaseEntity.java    people/RolePermissionId.java
tenancy/Tenant.java  security/SuperAdmin.java  security/RefreshToken.java
people/{User,Role,Permission,RolePermission,Resident,FamilyMember}.java (+ enums)
property/{Block,Unit}.java            finance/{PaymentMethod,Bill,Payment}.java
complaints/{ComplaintCategory,Complaint,ComplaintUpdate}.java
maintenance/MaintenanceRequest.java   facilities/{Facility,FacilityBooking}.java
community/{Announcement,Event}.java   visitors/{Visitor,VisitorPreRegistration}.java
system/{Notification,ActivityLog}.java
…/(each package)/*Repository.java     (package-private where possible)
test: my/baiti/arch/ArchitectureTest.java · per-aggregate *RepositoryIT.java
```

**Expected architecture**
- `TenantScopedBaseEntity` (`@MappedSuperclass`): `@TenantId UUID tenantId` (Hibernate discriminator), created/modified timestamps and actor UUIDs with auditing listener. `TenantAwareBaseEntity` extends it and adds generated UUID `@Id`. `RolePermission` extends the scoped base directly with existing `(role_id, permission_id)` `@EmbeddedId`/`@MapsId`, so tenant/audit columns are inserted without inventing a UUID id column. `Tenant` and `SuperAdmin` remain global. Accepted implementation refinement, 29 October; DDL is unchanged.
- Entity idiom: `meeting/03_SPRINGBOOT_REFRESHER.md §3.1` — Lombok getters/setters, `@ManyToOne(fetch = LAZY)` everywhere, `@Enumerated(STRING)` enums matching the CHECK lists in 04 §4 exactly, `columnDefinition` only where needed (citext, numeric(12,2), jsonb via `JdbcTypeCode`).
- Repositories extend `JpaRepository<T, UUID>`; only aggregate roots get repositories exposed beyond the package (e.g. `ComplaintUpdate` is reached via `Complaint`); derived-query methods only for what smoke tests need now — module packets add theirs.
- ArchUnit rules (03 §3, all five): tenant-scoped inheritance, controller/repository layering (vacuous until controllers exist), cross-package repository boundaries, JWT-import restriction, UTC timestamps while retaining legitimate `LocalDate` calendar fields.

**Testing requirements**
- Per aggregate `*RepositoryIT` (native PG16 shared harness per 06 §3.6, explicitly provisioned synthetic tenant fixture, `set_config` tenant context via test helper): save→find round-trip; unique-constraint violation surfaces (`DataIntegrityViolationException`) for each 04 §4 unique; enum round-trip; `createdAt` populated. Current foundation fixtures use the private test administrator; they do not prove production onboarding. Docker/Testcontainers execution is deferred by the owner, 27 October 2026; no assertions are skipped.
- `ddl-auto: validate` passing IS the schema-mapping test — a mismatched column fails context startup.
- ArchUnit suite green.

**Definition of Done**
- [ ] All 26 relations mapped; `./mvnw verify` green (validate + smoke + ArchUnit); evidence captured.
- [ ] No relationship uses EAGER; no entity is exposed by a controller (none exist — rule armed for the future).
- [ ] Commits per domain group, user-approved.
