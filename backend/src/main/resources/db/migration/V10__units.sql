-- 04_DATABASE_DESIGN.md §4.3 - V10
CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    block_id UUID NOT NULL REFERENCES blocks(id) ON DELETE RESTRICT,
    unit_no TEXT NOT NULL,
    type TEXT NULL,
    floor_no TEXT NULL,
    status TEXT NOT NULL DEFAULT 'VACANT' CHECK (status IN ('OCCUPIED', 'VACANT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_units_tenant_block_unit UNIQUE (tenant_id, block_id, unit_no)
);

CREATE INDEX idx_units_tenant_id ON units(tenant_id);
CREATE INDEX idx_units_block_id ON units(block_id);
