# P07 — Frontend Shell

**Objective**: A production-shaped React shell: auth (memory access token + silent-refresh cookie flow), routing with guards, generated API types, i18n, testing rig — ending with a real login against the P05 backend.

**Gate on entry: G-ANEP** — do not begin without the recorded meeting outcome (`meeting/05_OUTCOME.md`): frontend ownership + stack ratification. If Anep owns frontend, this packet is his onboarding vehicle; pair-execute.

**Scope**
- In: scaffold, tooling, auth flow, layout shell, login page, dashboard placeholder, type generation, MSW test rig.
- Out: all domain screens (module packets).

**Dependencies**: P05 (live auth endpoints). **References**: `meeting/04_REACT_REFRESHER.md` (canonical idioms), 03_ARCHITECTURE §5/§7.4, 05_QUALITY §7–8.

**Files to create**
```
frontend/  (npm create vite@latest -- --template react-ts)
├── package.json  vite.config.ts  tsconfig.json  tailwind.config.js  .eslintrc  .prettierrc
├── src/main.tsx  src/App.tsx (QueryClientProvider + RouterProvider + i18n init)
├── src/api/client.ts            # axios: baseURL VITE_API_URL, withCredentials, X-Tenant-Slug (dev)
├── src/api/types.gen.ts         # GENERATED — never hand-edited
├── src/stores/auth.ts           # Zustand, NOT persisted: user, accessToken, permissions
├── src/router.tsx  src/components/{ProtectedRoute,Can}.tsx
├── src/components/layout/AppShell.tsx   # nav (permission-filtered), header, <Outlet/>
├── src/pages/{Login,Dashboard,ForcedPasswordChange}.tsx
├── src/i18n/{index.ts,en.json}
├── src/lib/queryClient.ts  src/hooks/useAuth.ts
├── tests/setup.ts  src/mocks/{handlers.ts,server.ts}   # MSW from OpenAPI examples
└── scripts: "generate:api": openapi-typescript ../backend/openapi.json -o src/api/types.gen.ts
```

**Expected architecture**
- Auth flow (03 §7.4): login sets access token in memory + server sets refresh cookie; axios request interceptor attaches `Authorization`; response interceptor on 401 → single-flight `POST /api/auth/refresh` (cookie) → retry once → else logout+redirect. App boot: silent refresh attempt before rendering protected tree.
- `<Can permission="BILL_MANAGE">` reads claims from the store; UI-only convenience (FR-RBAC-03 note in code).
- `must_change_password` users are force-routed to `ForcedPasswordChange`.
- ESLint: `no-literal-strings` on JSX (05 §8), react-hooks rules, no-`any` warning; all copy in `en.json`.
- Accessibility per 05 §7 from the first screen (labelled inputs, focus ring, keyboard path).

**Testing requirements**
- Vitest+RTL: login form Zod validation; interceptor refresh-then-retry (MSW); logout on refresh failure; ProtectedRoute redirect; `<Can>` gating; forced-password-change routing.
- Manual evidence: browser login round-trip against dev backend, per `docs/ai-context/06_TESTING_EVIDENCE/browser_verification.md` Phase-5 flow table (all six rows).

**Definition of Done**
- [ ] `npm run lint && npm run typecheck && npm test && npm run build` all green (captured).
- [ ] Login → dashboard → refresh (wait/force) → logout works against local backend; evidence filed.
- [ ] `types.gen.ts` produced by script, not hands; no string literals in JSX outside i18n.
- [ ] Commits user-approved.
