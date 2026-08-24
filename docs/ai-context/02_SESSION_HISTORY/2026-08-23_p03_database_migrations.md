# Session — 2026-08-23 · P03 Database Migrations (V1–V28)

> **Agent / Author**: Antigravity AI · **Reviewed by**: Faisal
> **Project**: BAiTI (Multi-Tenant Condominium Management)
> **Branch**: `main`
> **Context on entry**: Completed P02 backend skeleton and schema designs in `docs/blueprint/04_DATABASE_DESIGN.md`.

## Context & Objectives
Transcribe and implement all 28 Flyway SQL database migrations in `backend/src/main/resources/db/migration/` implementing PostgreSQL 16 schema, tenant row-level security (RLS), and append-only audit protections.

## Work Accomplished (Who, When, What)
- **Author**: Antigravity AI (Reviewed manually by Faisal)
- **Date**: 23 August 2026
- **Migrations Created (V1–V28)**:
  1. `V1__baseline_extensions_and_grants.sql` — Enabled `citext` extension, configured schema permissions for `baiti_app`.
  2. `V2__tenants.sql` to `V8__refresh_tokens.sql` — Core platform tables: `tenants`, `super_admins`, `permissions`, `roles`, `roles_permissions`, `users`, `refresh_tokens`.
  3. `V9__blocks.sql` to `V15__payments.sql` — Property and finance tables: `blocks`, `units`, `residents`, `family_members`, `payment_methods`, `bills`, `payments`.
  4. `V16__complaint_categories.sql` to `V27__activity_logs.sql` — Operations and system tables: `complaint_categories`, `complaints`, `complaint_updates`, `maintenance_requests`, `facilities`, `facility_bookings`, `announcements`, `events`, `visitor_pre_registrations`, `visitors`, `notifications`, `activity_logs`.
  5. `V28__enable_rls.sql` — Enforced `ROW LEVEL SECURITY` (`FORCE`) across all 24 tenant-scoped tables with `USING/WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid)` policies and revoked `UPDATE, DELETE` grants on `payments` and `activity_logs`.

## Verification & Commits
- **Commits**:
  - `4e7e257` — *Feat(Database): Implement baseline extensions and core platform migrations V1-V8.*
  - `807e560` — *Feat(Database): Implement property and financial domain migrations V9-V15.*
  - `f82927c` — *Feat(Database): Implement operations migrations V16-V27 and RLS enforcement V28.*
  - `358f209` — *Docs(State): Advance module state to P03 complete in BAiTI.*

## State on Exit & Next Steps
- P03 migrations complete.
- Next step: P04 core JPA domain entities and Spring Data repositories.
