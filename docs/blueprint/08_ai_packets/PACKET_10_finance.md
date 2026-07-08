# P10 — Finance Module (payment methods, bills, payments)

**Objective**: The money trail: issue bills, record immutable payments with proof upload, correct balances under concurrency, resident self-service views. Financial correctness bugs are SEV-1 by definition.

**Scope**
- In: payment methods CRUD (FR-FIN-05), bills issue/cancel/list (FR-FIN-01), payments record/list (FR-FIN-02/03), resident views (FR-FIN-04), `StorageService` + local-FS implementation (first consumer), money-handling rules (FR-FIN-06).
- Out: overdue job + arrears view (FR-FIN-07 → P17), gateways (FR-FIN-08), refunds/compensating-entry UI (backlog — model supports it).

**Dependencies**: P09. **Requirements**: FR-FIN-01…06 · matrix rows `BILL_MANAGE`, `BILL_READ_ALL/OWN`, `PAYMENT_RECORD`, `PAYMENT_READ_OWN`, `PAYMENT_METHOD_MANAGE`.

**Files to create** — backend `my.baiti.finance/`: `PaymentMethodController` (`/api/payment-methods`), `BillService`, `BillController` (`/api/bills`), `PaymentService`, `PaymentController` (`/api/bills/{id}/payments` create; `/api/payments` reads), DTOs; `my.baiti.common/storage/{StorageService,LocalStorageService}.java` + upload config; tests `FinanceModuleIT`, `PaymentConcurrencyIT`. Frontend: `pages/finance/{BillsPage,BillDetailPage,MyBillsPage,PaymentMethodsPage}.tsx`, `components/finance/{BillForm,PaymentForm(with file input),BillStatusChip}.tsx`, hooks + specs.

**Expected architecture** (deltas)
- **Balance invariant** (04 §4.5): `PaymentService.record()` opens the transaction, `SELECT … FOR UPDATE` the bill, validates status payable + `amount_paid ≤ outstanding`, inserts payment, recomputes bill status — all-or-nothing. Overpay ⇒ 409 problem+json with `outstandingBalance` in detail.
- Payments append-only: no update/delete endpoints; controller layer doesn't even map them (DB grants already revoke — P03).
- `BigDecimal` end-to-end; JSON serialises amounts as strings; TS side never does float arithmetic (display only) — FR-FIN-06.
- Duplicate bill (unit+month+type) ⇒ 409. Cancel requires reason; paid bills uncancellable ⇒ 409.
- Proof upload: multipart ≤ 5 MB, jpg/png/pdf allowlist (magic-bytes check, not extension only), stored via `StorageService` under generated key; download endpoint streams with fixed content-type + `Content-Disposition: attachment`; own-scope enforced (a resident downloads only own proofs).
- Events: `BILL_CREATED` → notification to the unit's active residents (FR-NOT-01) + audit; `PAYMENT_RECORDED` → audit + notification to payer.

**Testing requirements**: README §3 set, plus: **`PaymentConcurrencyIT`** — two parallel payments against the same nearly-paid bill (executor + latch): exactly one succeeds, balance never negative, statuses consistent. Partial→exact-cover→PAID progression; upload rejection matrix (size/type/spoofed magic bytes); resident sees own units' bills only; Manager cancel/paid rules.

**Definition of Done**: README §3 module DoD + journey J2 end-to-end in browser (issue → notify → resident views → record with proof → PAID) evidenced; concurrency test green in CI mode (not just locally).
