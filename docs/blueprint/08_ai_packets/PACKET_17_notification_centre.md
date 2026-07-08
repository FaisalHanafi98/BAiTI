# P17 — Notification Centre & Scheduled Jobs (v1.x)

**Objective**: Surface the notification records accumulated since P05 (bell + centre), add the overdue-bills job, and formalise the channel interface with an email stub.

**Scope**: notification list/mark-read/unread-badge (FR-NOT-02); `NotificationChannel` SPI + `LogOnlyEmailChannel` stub (FR-NOT-03); overdue job + arrears view (FR-FIN-07); scheduled-job infrastructure (also hosting P16's expiry job). Out: real SMTP/SMS delivery, user notification preferences, digests.

**Dependencies**: P18 epic gate — technically P10. **Requirements**: FR-NOT-01…03, FR-FIN-07 · matrix row `NOTIFICATION_READ_OWN`.

**Files** — backend `my.baiti.system/`: `NotificationController` (`/api/notifications`, `POST …/{id}/read`, `POST …/read-all`, `GET …/unread-count`), `channel/{NotificationChannel,LogOnlyEmailChannel}.java`, `jobs/{ScheduledJobConfig,OverdueBillsJob}.java`; `finance`: arrears query + `GET /api/bills/arrears`; tests `NotificationCentreIT`, `OverdueJobIT`. Frontend: `components/layout/NotificationBell.tsx` (badge = unread-count polling via TanStack Query interval), `pages/NotificationsPage.tsx` + hooks/specs.

**Architecture deltas**: jobs iterate tenants explicitly and set tenant context per tenant (ThreadLocal doesn't exist in schedulers — reuse the I-8 event pattern: context set → work → clear, per tenant loop); `OverdueBillsJob` nightly: UNPAID past due ⇒ OVERDUE + notification to unit residents — idempotent (re-run changes nothing). `ScheduledLock` (shedlock) if the platform ever runs 2 replicas — note, not build, unless replicas exist. Channel SPI: `send(Notification)` — in-app writer is one implementation; email stub logs a structured line (proves the seam, delivers nothing).

**Testing**: README §3 set + job idempotency + job tenant-scoping proof (two tenants' overdue bills each notified under own tenant — I-8 for schedulers) + mark-read own-scope (user A can't read/mark user B's) + badge count correctness.

**DoD**: README §3 module DoD + browser evidence of bell badge → centre → mark read + an overdue transition produced by a clock-forced job run.
