-- 04_DATABASE_DESIGN.md §4.4 - V11
CREATE TABLE residents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id UUID NULL REFERENCES users(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    phone TEXT NULL,
    email CITEXT NULL,
    resident_type TEXT NOT NULL CHECK (resident_type IN ('OWNER', 'TENANT')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MOVED_OUT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_residents_tenant_id ON residents(tenant_id);
CREATE INDEX idx_residents_unit_id ON residents(unit_id);
CREATE INDEX idx_residents_user_id ON residents(user_id);
CREATE UNIQUE INDEX uq_residents_active_portal_account ON residents(tenant_id, user_id) WHERE user_id IS NOT NULL AND status = 'ACTIVE';
