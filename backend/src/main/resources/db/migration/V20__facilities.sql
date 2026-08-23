-- 04_DATABASE_DESIGN.md §4.7 - V20
CREATE TABLE facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    location TEXT NULL,
    capacity INTEGER NULL CHECK (capacity > 0),
    rules TEXT NULL,
    status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_facilities_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_facilities_tenant_id ON facilities(tenant_id);
