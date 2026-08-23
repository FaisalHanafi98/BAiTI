-- 04_DATABASE_DESIGN.md §6 - V1
CREATE EXTENSION IF NOT EXISTS citext;

-- Default privileges for baiti_app (app runtime role) on future tables created by baiti_migrator
ALTER DEFAULT PRIVILEGES FOR ROLE baiti_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO baiti_app;
ALTER DEFAULT PRIVILEGES FOR ROLE baiti_migrator IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO baiti_app;
