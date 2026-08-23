-- 04_DATABASE_DESIGN.md §4.6 - V16
CREATE TABLE complaint_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    description TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_complaint_categories_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_complaint_categories_tenant_id ON complaint_categories(tenant_id);
