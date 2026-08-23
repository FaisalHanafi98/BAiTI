-- 04_DATABASE_DESIGN.md §4.2 - V4
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    code TEXT NOT NULL,
    module TEXT NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_permissions_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_permissions_tenant_id ON permissions(tenant_id);
