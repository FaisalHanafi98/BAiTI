# UI/UX, motion and accessibility skills implementation — 17 November 2026

## Summary

Synced shared canonical workspace skill and reviewer pointers into `projects/baiti/.claude/` (`core@a68d4eb`) and applied the canonical `frontend-design-aesthetics`, `impeccable` and `ui-accessibility` rules across BAiTI's P07 authentication/workspace shell and P08 property screens on `igumi`.

## Changes

1. **Workspace skill pointer projection (`.claude/skills/`, `.claude/agents/`, `AGENTS.md`, `.gitignore`):**
   - Generated shared workspace pointers for `springboot`, `react-ui-patterns`, `ui-accessibility`, `frontend-design-aesthetics`, `design-for-ai`, `impeccable`, `foreman`, `tdd`, `code-commenting`, `verification-before-completion`, `database-migrations`, `security-review`, `systematic-debugging` and `humanizer`, plus `code-reviewer` and `security-reviewer`, while preserving BAiTI's custom `AGENTS.md` and `CLAUDE.md`.
2. **Semantic tokens, contrast, touch targets and motion craft (`frontend/src/styles.css`):**
   - Defined semantic CSS custom properties in `:root` for surfaces, text, high-contrast control borders (`#687d70`, `4.2:1` on white), primary/danger actions, notice/warning/danger callouts, `2.75rem` (`44px`) minimum interactive target sizing and `160ms cubic-bezier(0.22, 1, 0.36, 1)` ease-out motion.
   - Replaced implicit transitions with explicit compositor-safe `transform`, `opacity` and colour/border transitions (zero `transition: all`), added `:active:not(:disabled)` `scale(0.98)` tactile press feedback, origin-aware `.editor` panel entrance (`translateY(-0.25rem) scale(0.98)` → `scale(1)`) and `@media (prefers-reduced-motion: reduce)` parity.
   - Added `overflow-x: clip`, `min-width: 0` on flex/grid children and `overflow-wrap: anywhere` across headings, labels, cells, badges and callouts so compound Malay/English names and long slugs wrap without horizontal overflow at `375px`.
3. **Operational state distinction, occupancy semantics and keyboard Escape dismissal (`App.tsx`, `Forms.tsx`, `Property.tsx`, `Editors.tsx`):**
   - Distinguished confirmation notices (`data-tone="notice"`), unconfirmed server outcomes (`data-tone="warning"` for `logoutUnconfirmed` and `writeUnconfirmed`) and confirmed errors (`data-tone="danger"`).
   - Added `data-status="VACANT" | "OCCUPIED"` to unit occupancy badges so CSS pairs dashed vs solid borders and surface tokens with text labels instead of relying on colour alone.
   - Added `useEscapeDismiss` and trigger-button focus restoration so pressing `Escape` closes an open `BlockEditor`, `UnitEditor` or `DeletePanel` when no mutation is in flight and returns focus to the triggering button.

## Verification

See [2026-11-17_ui_skills_implementation.md](../06_TESTING_EVIDENCE/2026-11-17_ui_skills_implementation.md) for the Vitest, ESLint, TypeScript, Vite production build and multi-viewport Chrome/reduced-motion results.
