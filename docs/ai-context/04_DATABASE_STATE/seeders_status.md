# Development seed data

Updated 8 November 2026. `DevDataInitializer` uses the real onboarding
service to create two fictional communities after Flyway completes.

| Slug | Community | Initial admin |
|---|---|---|
| `baiti-dev-rumbia` | Rumbia Gardens | `baiti-dev-rumbia@example.invalid` |
| `baiti-dev-kenari` | Kenari Grove | `baiti-dev-kenari@example.invalid` |

Each community receives its default roles, permissions and initial admin.
The admin must change the password at first use. Each community has its
own onboarding transaction. Restarting skips existing active communities
with the expected names and preserves changed credentials.

Use a separate PostgreSQL 16 loopback database whose name begins
`baiti_dev_`. The database owner must be `baiti_migrator`, and the owner
must set a database comment of `baiti-dev:<UUID>`. Configure the regular
dev database, app and migrator credentials, JWT secret and explicit CORS
origin. Supply the following opt-in settings through the private dev
environment.

| Setting | Value |
|---|---|
| `BAITI_DEV_SEED_ENABLED` | `true` |
| `BAITI_DEV_SEED_DB_TOKEN` | UUID from the dev database comment |
| `BAITI_DEV_SEED_OPERATOR_ID` | ID of an existing active synthetic platform account in that database |
| `BAITI_DEV_SEED_INITIAL_PASSWORD` | Private initial password, at least 12 characters and at most 72 UTF-8 bytes |

Start with the `dev` Spring profile. The opt-in runner is absent when
`test` or `prod` is active. Use fictional records and reserved example
domains for every fixture. Keep runtime credentials and transcripts with
secrets outside Git. The [native test guide](../../testing/NATIVE_POSTGRES.md)
describes the separate test database.
