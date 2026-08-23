-- 04_DATABASE_DESIGN.md §4.2 - V5
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    description TEXT NULL,
    is_system BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_roles_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);
