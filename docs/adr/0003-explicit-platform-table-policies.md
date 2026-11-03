# 0003 — Explicit policies for the privileged platform tables

Date: 29 October 2026. Status: implemented and locally verified on
`test/persistence-acceptance`; no production execution or HTTP platform surface
is authorised by this document.

## Problem and selected boundary

The native characterization proves that FORCE RLS binds the ordinary
`baiti_migrator` owner. Neither it nor `baiti_app` can currently insert tenants
or super-admin records. Architecture section 6.5 already assigns platform
operations and the reviewed onboarding exception to a separate migrator-role
datasource, but the global policies do not supply that intended access.

Keep the two existing roles. Add explicit SELECT, INSERT and UPDATE policies
on `tenants` and `super_admins`, each **TO baiti_migrator**. Do not add DELETE,
FOR ALL, PUBLIC or app-role policies. Retain ENABLE/FORCE RLS, role attributes,
tenant-table policies, append-only grants and existing ownership. The app's
tenant self-row SELECT remains scoped; it gets no super-admin access.

This role is already a trusted schema owner with DDL authority. These policies
make its intended data access explicit; they do not make an owner incapable of
changing the schema. The future privileged datasource must stay outside the
ordinary runtime pool and behind P05's separate platform authentication chain.
No production authentication or onboarding service is added in this increment.

## Work order and acceptance

Source authority is database blueprint section 5. Record the policy contract
there before adding forward migration V30. Never edit V1–V29. Use a new owned
synthetic V29 target, preserving the earlier foundation/runtime databases.

First reproduce the migrator insert failure under its actual non-superuser,
NOBYPASSRLS connection. After the repair, prove owner SELECT/INSERT/UPDATE and
continued DELETE denial for both global tables. Preserve app insert denial;
add non-empty global-admin canaries and actual app-role escalation denial.
Verify exactly the six intended role-specific policies and unchanged domain
RLS/grants. Tests roll back their own rows; explicitly labelled shared synthetic
fixtures may be provisioned separately through the intended privileged role.

Run the complete native suite, an actual V29→V30 upgrade, fresh migration checks
and source/checksum preservation. Continue the missing persistence constraint
acceptance separately within this foundation-acceptance work. Do not mark P05,
full I1–I9, JWT, onboarding, coverage or hosted CI complete.

## Recovery

V30 is transactional PostgreSQL policy DDL without a backfill or table change.
If compensating changes are needed after application, use a new forward
migration to remove the named policies; preserve existing global records and
the migration ledger. Never reset a prior test database or rewrite applied SQL.

## Verification result

The two intended owner-provisioning tests failed before V30 with the actual
RLS denials. After the repair, all 111 tests pass on the V29→V30 fixture and a
new empty V1–V30 target: 58 unit/architecture and 53 integration tests, zero
failures/errors/skips. Six policy definitions name only the migrator and the
intended actions. Existing tenant records survive, and V1–V29 bytes are unchanged.

A named synthetic admin was inserted through the ordinary migrator for the
non-empty canary tests. App read/update/delete and SET ROLE escalation remain
denied; the migrator remains tenant-filtered on domain tables and cannot delete
global rows through DML. All per-test rows roll back. The shared fixture is not
a production login or proof of the future onboarding transaction.

See [the audit](../audits/persistence-acceptance-2026-10-29/VERIFICATION.json)
for the actual targets, commands, hashes and remaining acceptance gaps.

Scoped review: no blocking findings in the reviewed SQL-policy boundary. Only
the existing trusted role receives the named global actions; app/owner negative
controls, preserved FORCE flags and unchanged domain policies are exercised.
This verdict does not cover the unimplemented production API/authentication path
or constitute a dependency/security audit of the application.
