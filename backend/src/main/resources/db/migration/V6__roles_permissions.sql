-- 04_DATABASE_DESIGN.md §4.2 - V6
CREATE TABLE roles_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_roles_permissions_tenant_id ON roles_permissions(tenant_id);
CREATE INDEX idx_roles_permissions_permission_id ON roles_permissions(permission_id);
