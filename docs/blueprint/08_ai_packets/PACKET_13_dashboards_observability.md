# P13 — Dashboards & Observability Hardening

**Objective**: Role dashboards over the accumulated data, plus the observability the runbook depends on — closing milestone M4 into M5.

**Scope**
- In: role-based dashboard (counts/aggregates), Micrometer custom metrics (05 §9 list), log-correlation polish, k6 scripts + recorded baseline, the "< 2 min trace drill".
- Out: external Grafana/alerting infrastructure (P18 wires the target platform's version); report exports.

**Dependencies**: P10, P11, P12.

**Files to create** — backend `my.baiti.dashboard/`: `DashboardService`, `DashboardController` (`GET /api/dashboard` — role-shaped payload), custom metric registrations (`baiti_login_failures_total`, `baiti_listener_failures_total`, `baiti_rls_missing_context_total`); `perf/k6/{load.js,stress.js,soak.js}` + `perf/README.md`. Frontend: `pages/Dashboard.tsx` replaces placeholder — stat cards + recent-activity list per role. Docs: `docs/testing/PERF_BASELINE.md`, `docs/deployment/OBSERVABILITY_RUNBOOK.md` (trace-drill procedure).

**Expected architecture (deltas)**
- One endpoint, role-shaped response (not per-role endpoints): Admin/Manager → open complaints, pending maintenance, unpaid/overdue bills, occupancy %; Resident → my outstanding balance, my open items; Guard (v1.0) → placeholder pending P16. Aggregates are plain indexed queries under RLS (02 §6.3) — measure, don't cache.
- MDC/`X-Request-Id` verified end-to-end (client sends none ⇒ generated; response echoes).
- k6 per 06 §7 with the seeded-volume script; baseline numbers committed.

**Testing requirements**: slice tests per role payload (counts match seeded fixtures); matrix rows for the endpoint; k6 targets met (06 §7); drill rehearsal: given a dashboard requestId from the browser, produce the full log trace — timed, documented.

**Definition of Done**: README §3 DoD where applicable + `PERF_BASELINE.md` filled with real numbers + drill ≤ 2 min evidenced + dashboards demonstrated per role in browser.
