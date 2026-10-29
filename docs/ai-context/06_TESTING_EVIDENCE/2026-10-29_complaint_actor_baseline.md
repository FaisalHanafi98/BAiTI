# Complaint-history baseline correction — 29 October 2026

The existing working-tree V18 correction predates the native-harness work. HEAD
defines `updated_by` twice in `complaint_updates`: once as the required domain
actor and once as the nullable audit stamp. The correction names the actor
`user_id` and adds its index. ComplaintUpdate already maps its user association
to `user_id`; the schema blueprint now records that distinction explicitly.

Scope: V18's existing correction, only the complaint-actor portions of the
database blueprint, and this evidence note. Other blueprint changes, including
the V29 tenant-setting policy, belong to separate commits.

Verification already performed:

- A clean HEAD backend export with the V18 correction and the separately scoped
  native test harness applied V1–V28 on PostgreSQL 16.15 and passed 46 unit plus
  9 integration tests, with no failures, errors or skips. Captured output:
  `docs/audits/foundation-2026-10-29/24-atomic-native-stage.log` (ignored raw log).
- The complete working tree subsequently passed 57 unit/architecture plus 38
  integration tests, with no failures, errors or skips. Captured output:
  `docs/audits/foundation-2026-10-29/31-final-native-suite.log` (ignored raw log).
  That run also includes the later mapping and V29 changes; it is not an
  isolated test of this proposed commit.
- `git diff --check` passes for the migration and database blueprint. Direct
  inspection confirms the actor column matches ComplaintUpdate's mapping.

The correction was already present before these private test databases were
created. Their applied V18 bytes were not edited, reset or repaired in Flyway
history. Other environments' applied histories remain unverified. P05, full
G-RLS, hosted CI and deployment are outside this correction's verification.
