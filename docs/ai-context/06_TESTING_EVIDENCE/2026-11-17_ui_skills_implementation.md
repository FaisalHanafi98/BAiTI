# UI skills and accessibility verification — 17 November 2026

All checks ran from the unified `baiti` checkout on `igumi`.

| Check | Result |
| --- | --- |
| Workspace `core` unit suite (`python -m unittest discover -s ../../core/tests -v`) | 91 passed in 35.66s; workspace root and BAiTI `.claude/` skill pointers synced at `core@a68d4eb`. |
| Frontend unit/integration suite (`npm.cmd --prefix frontend test`) | 9 passed across `tests/session.test.ts` (5) and `tests/routes.test.tsx` (4), including `Escape` dismissal with focus return, `data-status` occupancy attributes and `data-tone="warning"` unconfirmed-write callouts. |
| Frontend static gates (`lint -- --max-warnings 0`, `typecheck`, `build`) | ESLint passed with 0 errors and 0 warnings; `tsc --noEmit` passed; `vite build` succeeded in 3.00s. |
| Chrome multi-viewport and reduced-motion check (`375x812`, `768x1024`, `1280x800`, `prefers-reduced-motion: reduce`) | Zero horizontal overflow (`scrollWidth <= clientWidth`) on `/login` and `/property` with compound Malay/English block names; buttons and inputs measured `45.19px` height and nav links `44px` (`>= 44px` target); explicit compositor/colour transition properties (`transform, background-color, border-color, color, opacity`); `Escape` closed `BlockEditor` and returned focus to `Add block`; `transitionDuration` collapsed to `1e-05s` under `prefers-reduced-motion: reduce`. |
