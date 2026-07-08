#!/bin/bash
# BAiTI dev-DB role bootstrap — runs ONCE at first container start (postgres entrypoint).
# Creates the two-role model required for RLS to actually bind (blueprint 03_ARCHITECTURE §6.2):
#   baiti_migrator — schema owner, used by Flyway only
#   baiti_app      — runtime role, NOBYPASSRLS, subject to every RLS policy
# Prod bootstrap runs the same statements manually (docs/deployment/, P18).
# Note: a .sh (not .sql) because init SQL files cannot read environment variables.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE baiti_migrator LOGIN PASSWORD '${DB_MIGRATOR_PASSWORD}';
    CREATE ROLE baiti_app LOGIN PASSWORD '${DB_APP_PASSWORD}' NOBYPASSRLS;
    ALTER DATABASE ${POSTGRES_DB} OWNER TO baiti_migrator;
    ALTER SCHEMA public OWNER TO baiti_migrator;
    GRANT USAGE ON SCHEMA public TO baiti_app;
EOSQL

echo "BAiTI roles created: baiti_migrator (owner), baiti_app (NOBYPASSRLS)"
