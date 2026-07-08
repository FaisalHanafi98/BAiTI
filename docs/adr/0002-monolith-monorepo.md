# ADR-0002 — Modular Monolith in a Monorepo

**Status**: Accepted · **Full rationale**: `docs/blueprint/03_ARCHITECTURE.md §1–2` (this ADR is the durable pointer)

## Decision

One Spring Boot deployable (`backend/`, package-by-feature under `my.baiti`, boundaries enforced by ArchUnit tests) + one React SPA (`frontend/`) + shared infra and docs, in a single repository. Shared-schema multi-tenancy on one PostgreSQL cluster, row-isolated via RLS.

## Why

- Two part-time developers: a monolith maximises velocity and keeps critical transactions (tenant onboarding, bill+payment) single-boundary.
- Monorepo keeps schema, API contract, and generated client types in lockstep — one PR changes all three.
- Package-by-feature + interface-only cross-calls keeps a future service-extraction path open without paying distribution costs now.
- Schema-per-tenant was rejected: migration fan-out and pool fragmentation at the 50-tenant design ceiling outweigh its isolation gains, which RLS already provides at the row level.

## Revisit triggers

A whale tenant dominating load (extraction seam documented at `TenantAwareDataSource`), or a team size where independent deploys beat shared velocity. Any such move requires a superseding ADR.
