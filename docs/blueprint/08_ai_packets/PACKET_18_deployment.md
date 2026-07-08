# P18 — Deployment & Go-Live

**Objective**: BAiTI v1.0 serving real traffic on the chosen target with backups, rollback, and a runbook — the M6 milestone.

**Scope**
- In: Dockerfiles, target provisioning (staging + prod), env/secrets wiring, DB bootstrap on target, backup automation, restore + rollback rehearsals, runbook, go-live checklist execution, CD activation (from P06 stubs).
- Out: custom domains/white-labelling; multi-region; blue-green (documented as post-v1.0 — 05 §5).

**Dependencies**: P13 + M5 exit (perf/security/UAT evidence) + **decision D3 made** (03 §12 matrix; default recommendation Fly.io SG unless the user rules otherwise) + G-PUSH long since resolved.

**Files to create**
```
infra/Dockerfile.backend      # multi-stage: maven build → JRE-17 slim, non-root user
infra/Dockerfile.frontend     # build → nginx static (or platform static hosting)
infra/nginx.conf              # SPA fallback, gzip, cache headers, security headers
.github/workflows/cd.yml      # activate P06 stub: dev auto → staging/prod manual approval
docs/deployment/{DEPLOYMENT_GUIDE.md,RUNBOOK.md,GO_LIVE_CHECKLIST.md,BACKUP_RESTORE.md}
scripts/backup/{nightly_dump.sh, restore_drill.sh}
```

**Expected architecture (deltas)**
- Target bootstrap mirrors P01's role model: `baiti_migrator` owns schema, app runs as `baiti_app` — verify I-7 canary passes **on the target DB** (managed Postgres roles differ; this is where RR-2 bites in prod).
- Secrets via platform secret store; `.env.example` parity check scripted (missing var ⇒ deploy job fails).
- Tenant resolution flips to `SubdomainTenantResolver` (prod profile); wildcard DNS + TLS on the platform.
- Backups per 03 §13 (nightly dump, 30-day retention, off-site); `restore_drill.sh` restores latest into a scratch container + runs smoke suite.
- Runbook contents: start/stop/scale, log access, trace drill, JWT secret rotation, DB failover/restore, tenant onboarding SOP, incident severity ladder (root SOP §11 alignment).

**Testing requirements**: staging smoke = journeys J1–J3 scripted against staging with two fresh tenants; restore rehearsal executed with timing recorded (RTO proof); rollback rehearsal: deploy N-1 tag, verify health, re-deploy N; go-live checklist (06 §9 gates + 05 §5 items) signed off line-by-line with evidence links.

**Definition of Done**
- [ ] Prod up: health UP, isolation canary green on prod DB, seed-free (no synthetic tenants in prod).
- [ ] First real tenant onboarded via the runbook SOP.
- [ ] Backups observed running two consecutive nights; restore + rollback rehearsal evidence filed.
- [ ] `GO_LIVE_CHECKLIST.md` fully checked with links; `docs/ai-context/` updated to LIVE status.
