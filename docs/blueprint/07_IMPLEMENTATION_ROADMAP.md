# BAiTI — Implementation Roadmap

> **Blueprint document 7 of 9** | Produced: 2026-07-07
> Work is packaged as AI implementation packets (P00–P18, defined in `08_ai_packets/`); this document sequences them into milestones with gates, dependencies, risks, estimates, and rollback rules. **No packet may start before its dependencies' Definition of Done is evidenced.**

---

## 1. Epic Map

| Epic | Packets | Delivers |
|---|---|---|
| E1 Foundation | P00–P03 | verified env · governed repo · runnable skeleton · migrated schema |
| E2 Security core | P04–P05 | entities/repos · tenancy+auth+RBAC · **isolation proven** |
| E3 Delivery rails | P06–P07 | CI/CD · frontend shell with auth |
| E4 v1.0 modules | P08–P12 | property · people · finance · complaints · maintenance |
| E5 Hardening | P13 + M5 activities | dashboards · observability · perf/security/UAT passes |
| E6 Go-live | P18 | staging · production · DR rehearsal · runbook |
| E7 v1.x modules | P14–P17 | facilities · community · visitors · notification centre |

## 2. Milestones, Gates & Exit Criteria

```
M0 ──▶ M1 ──▶ M2 ══[G-RLS]══▶ M3 ──▶ M4 ──▶ M5 ──▶ M6(v1.0 live) ──▶ M7(v1.x)
              ▲                 ▲
        [G-NAME done in P01]  [G-ANEP before P07 FE work]   [G-PUSH before any remote]
```

**Gates (hard stops):**
- **G-NAME** — rename decision executed: folder `baiti`, ADR-0001 recorded (P01, needs user confirmation to rename the folder).
- **G-RLS** — the entire isolation suite I-1…I-9 green in CI. **No feature module (P08+) may begin before this** — root instruction from the launch prompt, now enforced by packet dependencies.
- **G-ANEP** — Anep meeting outcome recorded (`meeting/05_OUTCOME.md`): stack ratified, frontend ownership assigned, MVP split confirmed (SRS ASM-2). Blocks P07+ frontend packets only; backend continues regardless.
- **G-PUSH** — GitHub location/visibility decided (D2). Blocks the first `git push`/repo creation only; local commits proceed from P01. Per CON-4, no push ever happens without explicit user instruction.

| Milestone | Contains | Exit criteria (all evidenced per 06 §10) |
|---|---|---|
| **M0** Environment | P00 | `java 17+`, `mvn`, `node 18+`, `docker ps`, `git`, `gh` verified; output logged to `docs/audits/PHASE0_VERIFICATION.md` |
| **M1** Foundation | P01, P02, P03 | local repo with governed structure; `mvn verify` green incl. health test; all 28 migrations apply on Testcontainers; migration test green; ArchUnit baseline green |
| **M2** Security core | P04, P05 | all entities+repos with smoke tests; login/refresh/logout/me working; onboarding atomic; **G-RLS passed** |
| **M3** Rails | P06, P07 | CI green on a test PR (both workflows); branch protection on; FE login → protected dashboard against local BE; generated API types wired |
| **M4** v1.0 modules | P08–P12 | each module: BE slice tests + FE screens + matrix rows + I-suite still green; journeys J1–J3 pass on dev |
| **M5** Hardening | P13 + §7/§8 of 06 | k6 targets met; security pass clean; UAT exit criteria met; observability drill < 2 min |
| **M6** Go-live | P18 | staging + prod up on chosen target (D3 decided); restore rehearsal evidenced; runbook exists; rollback rehearsed |
| **M7** v1.x | P14–P17 | facilities/community/visitors/notification-centre live; J4 passes |

## 3. Dependency Rules (machine-checkable)

```
P00 ◀ P01 ◀ P02 ◀ P03 ◀ P04 ◀ P05
P05 ◀ P06        (CI needs the full test suite to be meaningful)
P05 ◀ P07        (FE auth needs real endpoints)  + G-ANEP
P05 ◀ P08 ◀ P09 ◀ P10          (people screens need property; finance needs both)
P09 ◀ P11, P12                  (complaints/maintenance need residents/users)
P10,P11,P12 ◀ P13               (dashboard aggregates need data models)
P13 ◀ P18 ◀ P14..P17            (go-live before v1.x epics)
```
Inside every module packet: backend slice first (tests green), then frontend consuming the frozen contract. Two-person parallelism: after M3, Faisal runs Pn backend while Anep runs Pn-1 frontend.

## 4. Complexity & Calendar Estimate

Basis: CON-3 (~12 h/week combined), packet sizes S ≈ ≤6 h, M ≈ 6–15 h, L ≈ 15–30 h.

| Packet | Size | Packet | Size |
|---|---|---|---|
| P00 env | S | P08 property | M |
| P01 bootstrap | S | P09 people | L |
| P02 backend skeleton | M | P10 finance | L |
| P03 migrations | L | P11 complaints | L |
| P04 entities/repos | L | P12 maintenance | M |
| P05 security+tenancy | **XL (split into 5 sub-slices in the packet)** | P13 dashboards/obs | M |
| P06 CI/CD | M | P18 deployment | L |
| P07 FE shell | L | P14–P17 (each) | M–L |

Calendar (with the meeting brief's honest 30 % buffer): **M0–M2 ≈ 5–6 wks · M3 ≈ 2 wks · M4 ≈ 8–10 wks · M5–M6 ≈ 3–4 wks → v1.0 live ≈ 4.5–5.5 months**; M7 adds ~6–8 wks. The old "3–4 months to MVP" claim is superseded — it ignored hardening and go-live.

## 5. Consolidated Risk Register (supersedes plan §9 and audit §6 lists)

| ID | Risk | L×I | Mitigation / owner |
|---|---|---|---|
| RR-1 | **Cross-tenant leak** | L×CRIT | 3 layers + I-suite as permanent CI gate (G-RLS); 404-not-403 rule |
| RR-2 | **RLS silently not binding (owner-bypass / BYPASSRLS misconfig)** — *new, found in discovery* | M×CRIT | dual DB roles + `FORCE` + I-7 canary asserts `baiti_app`+`rolbypassrls=false` every build |
| RR-3 | Tenant context bleed across pooled connections | M×CRIT | reset-on-return in `TenantAwareDataSource`; I-5/I-6 fail-closed proofs |
| RR-4 | Tenant lost in async listeners (audit rows mis-tenanted) | M×H | tenantId travels in event payload; I-8 |
| RR-5 | Anep expectations diverge (stack/scope/ownership) | M×H | G-ANEP gate; backend-first sequencing keeps his lane unblocked |
| RR-6 | Part-time capacity + scope creep | H×M | MoSCoW in SRS; packets sized ≤ evening chunks; v1.x strictly gated behind go-live |
| RR-7 | Testcontainers slow on Windows kills TDD loop | M×M | singleton container; suite budget 5 min enforced as a defect |
| RR-8 | Refresh rotation lockouts (race on parallel tabs) | L×H | family model tolerates one in-flight rotation; §3.4 tests; env-flag to disable rotation as break-glass |
| RR-9 | PDF typos/miscounts re-enter via careless generation | M×M | 04 §9 deviation register is normative; review checklist item |
| RR-10 | Editing applied migrations | M×M | immutability rule + checksum failure in CI |
| RR-11 | PDPA: real personal data in seeds/logs | L×H | synthetic-only rule; secret/PII scan; log-redaction review |
| RR-12 | Deployment target indecision stalls M6 | M×L | matrix ready (03 §12); decision needed only at M5 exit |
| RR-13 | Two-person bus factor / session amnesia | M×M | this blueprint + `docs/ai-context/` update protocol every session |
| RR-14 | JWT secret leak | L×CRIT | env-only, scanner, rotation runbook (accepted cost: global logout) |

## 6. Rollback Considerations by Stage

| Stage | Rollback story |
|---|---|
| Pre-push (M1–M2, local-only repo) | `git revert`/reset; nothing external exists — cheapest period to change course (that's why security core sits here) |
| Schema (all stages) | **Forward-fix only** (no Flyway undo): new `Vn+1` migration; pre-deploy backup mandatory; expand-migrate-contract for live changes |
| App releases (M6+) | previous image tag retained; redeploy = rollback; deploy checklist asks "does the new migration break the previous app?" |
| Feature misfire in prod | v1.0 has no flag framework (deliberate); rollback = redeploy previous tag; the one env flag: refresh-rotation disable (RR-8) |
| Failed go-live | staging is the rehearsal; go-live checklist includes restore drill + rollback drill *before* DNS cutover |

## 7. Standing Instructions for Every Packet Executor

1. Read `08_ai_packets/README.md` (protocol) + your packet + every blueprint doc it references. The blueprint overrides older repo docs (00 §2).
2. TDD: failing test first; the packet's acceptance criteria are the test names.
3. Never touch: applied migrations, generated `types.gen.ts`, another packet's scope ("no opportunistic improvement" — root SOP §4.5).
4. Every session ends with the `docs/ai-context/` updates (status + session history) and evidence attached.
5. Commit protocol per CON-4: draft message → user reviews → commit; **never push**.
6. When the packet conflicts with reality (compile error in a spec snippet, missing dependency), stop and record the deviation in the session log rather than improvising architecture.
