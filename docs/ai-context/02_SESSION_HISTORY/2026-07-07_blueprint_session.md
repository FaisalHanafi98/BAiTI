# Session — 2026-07-07 · Engineering Blueprint Programme

> **Agent**: Claude (Fable 5), Claude Code
> **Mandate**: Master SDLC planning & architecture programme (10 phases, no application code)
> **Result**: Complete engineering blueprint produced at `docs/blueprint/` (9 documents + 19 AI implementation packets)

## What was done

1. **Phase 1 discovery**: every repository file read, including the full data-model PDF. Inventory + contradiction ruling: `docs/blueprint/00_REPOSITORY_INVENTORY.md`.
2. **Phases 2–8**: SRS (`01`), system analysis with state machines/journeys/access matrix (`02`), architecture with normative tenancy design (`03`), full 26-relation database design with corrected V1–V28 migration sequence (`04`), quality attributes with STRIDE threat model (`05`), testing strategy with the I-1…I-9 isolation release gate (`06`), roadmap with milestones/gates/risks (`07`).
3. **Phase 9**: executor protocol + packets P00–P18 (`08_ai_packets/`).
4. **Phase 10**: master index (`docs/blueprint/README.md`), this session record, refreshed `01_CURRENT_STATUS.md`.

## Key findings (defects in prior planning, now corrected)

- PDF table count is 23 (docs said 22) → final schema 26 relations.
- 8 additional PDF typos beyond the old correction list (complete register: blueprint 04 §9).
- Migration-order defect: `refresh_tokens` created after the RLS migration; internal numbering conflict in the old plan.
- **RLS owner-bypass gap**: no doc separated the Flyway (owner) role from the runtime role — without `baiti_migrator`/`baiti_app` + `FORCE ROW LEVEL SECURITY`, RLS would silently not bind. This was the highest-value catch of the session (risk RR-2).
- `SET LOCAL` outside a transaction is a no-op → replaced with pool-safe set/reset datasource wrapper.
- `payments.payment_method` denormalization fixed to FK.

## Decisions taken (decided-but-reversible baselines, flagged in blueprint 00 §5)

D4 MYR-only + currency column · D5 EN-only but i18n-scaffolded · D6 separate `super_admins` table · templates-in-code for role/permission seeding · token storage = memory access + httpOnly refresh cookie · dev seeding via `DevDataInitializer` (no SQL seeds).

## Open items for the user

- G-NAME: confirm folder rename to `baiti`.
- G-ANEP: record meeting outcome (`meeting/05_OUTCOME.md`) — blocks frontend packets only.
- G-PUSH: GitHub location/visibility — blocks first push only.
- D3: deployment target — needed at M5 exit; matrix in blueprint 03 §12.

## State on exit

Zero code, no git repo (unchanged — by mandate). Next action: Packet P00. Resume protocol: `docs/ai-context/README.md`; planning source of truth: `docs/blueprint/`.
