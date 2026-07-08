# P04 — JPA Entities & Repositories

**Objective**: Every table mapped to a JPA entity with the tenant-aware base class, a Spring Data repository per aggregate, repository smoke tests, and the ArchUnit rule set activated.

**Scope**
- In: `TenantAwareBaseEntity`, all entities + enums, repositories, JPA auditing, `@TenantId` wiring (declaration only — the resolver arrives in P05), ArchUnit tests, repo smoke tests.
- Out: services, controllers, DTOs (module packets); TenantContext/resolvers (P05).

**Dependencies**: P03.

**Files to create** (package-by-feature under `my.baiti.…`)
```
common/TenantAwareBaseEntity.java     common/AuditorAwareImpl.java (stub → P05)
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
- `TenantAwareBaseEntity` (`@MappedSuperclass`): `@Id UUID id` (generated), `@TenantId UUID tenantId` (Hibernate 6 discriminator — 03 §6.1 layer 2), `@CreatedDate/@LastModifiedDate Instant`, `@CreatedBy/@LastModifiedBy UUID`, `@EntityListeners(AuditingEntityListener.class)`. `Tenant` and `SuperAdmin` do NOT extend it.
- Entity idiom: `meeting/03_SPRINGBOOT_REFRESHER.md §3.1` — Lombok getters/setters, `@ManyToOne(fetch = LAZY)` everywhere, `@Enumerated(STRING)` enums matching the CHECK lists in 04 §4 exactly, `columnDefinition` only where needed (citext, numeric(12,2), jsonb via `JdbcTypeCode`).
- Repositories extend `JpaRepository<T, UUID>`; only aggregate roots get repositories exposed beyond the package (e.g. `ComplaintUpdate` is reached via `Complaint`); derived-query methods only for what smoke tests need now — module packets add theirs.
- ArchUnit rules (03 §3, all five): entity-inheritance, controller/repository layering (trivially green now), cross-package via services only, JWT-import restriction, `Instant`-only temporal fields.

**Testing requirements**
- Per aggregate `*RepositoryIT` (Testcontainers, migrator-seeded tenant row, `set_config` tenant context via test helper): save→find round-trip; unique-constraint violation surfaces (`DataIntegrityViolationException`) for each 04 §4 unique; enum round-trip; `createdAt` populated.
- `ddl-auto: validate` passing IS the schema-mapping test — a mismatched column fails context startup.
- ArchUnit suite green.

**Definition of Done**
- [ ] All 26 relations mapped; `./mvnw verify` green (validate + smoke + ArchUnit); evidence captured.
- [ ] No relationship uses EAGER; no entity is exposed by a controller (none exist — rule armed for the future).
- [ ] Commits per domain group, user-approved.
