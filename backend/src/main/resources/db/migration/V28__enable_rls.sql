-- 04_DATABASE_DESIGN.md §5 & §6 - V28: Enable Row-Level Security & Policies

-- ============================================================================
-- 1. Tenant-Scoped Tables (24 tables): Enable & Force RLS + Isolation Policy
-- ============================================================================

-- permissions
ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE permissions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON permissions
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- roles
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON roles
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- roles_permissions
ALTER TABLE roles_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles_permissions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON roles_permissions
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- users
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- refresh_tokens
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON refresh_tokens
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- blocks
ALTER TABLE blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE blocks FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON blocks
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- units
ALTER TABLE units ENABLE ROW LEVEL SECURITY;
ALTER TABLE units FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON units
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- residents
ALTER TABLE residents ENABLE ROW LEVEL SECURITY;
ALTER TABLE residents FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON residents
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- family_members
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON family_members
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- payment_methods
ALTER TABLE payment_methods ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_methods FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payment_methods
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- bills
ALTER TABLE bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE bills FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON bills
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- payments
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON payments
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- complaint_categories
ALTER TABLE complaint_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE complaint_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON complaint_categories
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- complaints
ALTER TABLE complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE complaints FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON complaints
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- complaint_updates
ALTER TABLE complaint_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE complaint_updates FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON complaint_updates
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- maintenance_requests
ALTER TABLE maintenance_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE maintenance_requests FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON maintenance_requests
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- facilities
ALTER TABLE facilities ENABLE ROW LEVEL SECURITY;
ALTER TABLE facilities FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON facilities
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- facility_bookings
ALTER TABLE facility_bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE facility_bookings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON facility_bookings
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- announcements
ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON announcements
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- events
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON events
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- visitor_pre_registrations
ALTER TABLE visitor_pre_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE visitor_pre_registrations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON visitor_pre_registrations
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- visitors
ALTER TABLE visitors ENABLE ROW LEVEL SECURITY;
ALTER TABLE visitors FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON visitors
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- notifications
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notifications
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- activity_logs
ALTER TABLE activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON activity_logs
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::uuid);


-- ============================================================================
-- 2. Platform-Global Tables (tenants, super_admins)
-- ============================================================================

-- tenants: narrow self-row SELECT policy for baiti_app
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenants FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_self_select ON tenants
    FOR SELECT
    USING (id = current_setting('app.current_tenant', true)::uuid);

-- super_admins: RLS enabled with NO policy for baiti_app (deny-all default)
ALTER TABLE super_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE super_admins FORCE ROW LEVEL SECURITY;


-- ============================================================================
-- 3. Append-Only Grant Enforcement
-- ============================================================================
-- Revoke UPDATE and DELETE on immutable audit and payment ledgers from application role
REVOKE UPDATE, DELETE ON payments FROM baiti_app;
REVOKE UPDATE, DELETE ON activity_logs FROM baiti_app;
