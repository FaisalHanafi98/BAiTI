# Session — 2026-07-25 · Frontend foundation strategy (paper only)

**Agent**: Claude Code (Fable 5)
**Scope**: design/planning only — no code written, no packet started, nothing committed.

## What was produced

`docs/plans/FRONTEND_FOUNDATION_v1.md` (DRAFT) — the frontend foundation
strategy the eventual P07+ implementation should follow:

- IA: one SPA, three role-shaped surfaces (`/home` resident, `/console`
  management, `/gate` guard v1.x) + `/admin` super-admin; landing by
  permission set; tenant never in the URL.
- Wireflows for the five adoption journeys (bill, complaint, booking,
  visitor+guard, announcement), mobile-first, honest about v1.0
  record-not-process payments.
- Design-system foundation: two-layer tokens (semantic CSS variables under
  Tailwind), per-tenant theming (name+logo+accent only, contrast-enforced),
  form/collection/state-machine-chip standards, empty/loading/error/404
  standards including the fail-closed caveat.
- Technical foundation: package-by-feature mirroring `my.baiti.*`, state
  boundaries (Query/Zustand/RHF/URL), generated-contract discipline,
  React-Native portability guardrails as lint rules, testing lanes,
  performance budgets carried from the blueprint.
- Phased roadmap F0–F5, every phase gated (G-RLS, G-ANEP ratification,
  packet dependencies).
- Sign-off register: D-A1…D-A7 for Anep; D-F1 (public tenant-profile
  endpoint), D-F2 (BigDecimal-as-string in the API contract — must precede
  the first frozen contract), D-F3 (no-impersonation ADR note) for Faisal.

## Basis

Read in full: blueprint 01–05, 07, P07 packet, 04_DATABASE_DESIGN,
access-control matrix (02 §8), `meeting/04_REACT_REFRESHER.md` §11,
`meeting/05_OUTCOME.md`, `docs/ai-context/01_CURRENT_STATUS.md`.

## Open items created

1. Faisal + Anep review the draft; resolutions land in a v2 and the blueprint
   decision register (`00_REPOSITORY_INVENTORY.md §5`).
2. D-F2 (money serialisation) should be decided before P05/P07 freeze the
   OpenAPI contract.
3. Ecosystem: BAiTI still lacks `.ai/manifest.json` (ai-core onboarding);
   frontend sessions meanwhile need `ai pack` output for react-ui-patterns,
   ui-accessibility, frontend-design-aesthetics.

## Explicitly NOT done

No frontend scaffold, no commits, no changes to blueprint documents, no P02/P03
work. P02 DoD remains Docker-gated as recorded in `01_CURRENT_STATUS.md`.
