# 0002 — Move the backend to a supported Spring baseline

Date: 29 October 2026. Status: accepted for isolated implementation; the current
foundation still runs Spring Boot 3.3.5 until the upgrade passes verification.

## Context and decision

The owner authorises continued implementation and local commits after the shared
SOP and Humanizer pass. F1 includes a supported-version review before P05. Spring
states that [Boot 3.3 ended open-source support on 19 June 2025](https://spring.io/blog/2025/06/19/spring-boot-3-3-13-available-now/).
Building the new authentication layer on that line would add avoidable migration
work and leave the development baseline outside open-source support.

Target Boot **4.1.2**, shown as stable in the
[official project page](https://spring.io/projects/spring-boot/) and
[system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
checked today. Retain Java 17 source compatibility, the existing Java 21 runtime,
native PostgreSQL 16, Flyway history and dual roles. Revalidate the exact release
if this work is resumed later.

Use the [official migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide):
first check the latest 3.5 line, then the 4.x changes. The observed bridge is
3.5.16; it is a migration step, not the final supported target. Review changed
starter/test modules, Jackson and ORM behaviour. Select a compatible springdoc
release from its [official matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot):
Boot 4.1 maps to springdoc 3.1–3.2; 3.2.0 is the documented current stable choice.

## Bounded implementation and acceptance

Use an isolated checkout based on the verified foundation commit so earlier
research/file-organisation edits remain untouched. Scope is dependency/config
compatibility and required imports/API adaptations, plus meaningful regression
coverage. Keep it as a separate runtime-upgrade PR after the foundation PR.
No feature, Docker, hosting, database reset or remote audit is part of the work.

Before each step, preserve the passing 102-test baseline and record actual
failures. Verify the complete native suite, schema validation, exact existing
migration checksums, app/migrator identities, tenant boundary/append-only checks,
history retention and health. Add a runtime-version assertion so the test proves
the selected framework actually loads. Existing failures or skipped tests do not
establish a successful upgrade. Check packaged application startup and API docs
where their exposed contract changes.

## Recovery and remaining gates

The upgrade changes source/dependency declarations only. Retain the foundation
branch and test database; do not repair Flyway history or weaken a test to accept
new ORM behaviour. Failed experiments stay outside the foundation branch.
Dependency download access may be needed; it does not authorise uploading an
inventory or running a remote vulnerability audit.

Passing this upgrade will not close P05, the explicit platform-provisioning policy,
exhaustive constraint checks, full G-RLS, hosted CI or release readiness. Those
remain separate acceptance gates.
