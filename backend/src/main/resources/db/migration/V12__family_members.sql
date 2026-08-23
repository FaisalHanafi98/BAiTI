-- 04_DATABASE_DESIGN.md §4.4 - V12
CREATE TABLE family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    resident_id UUID NOT NULL REFERENCES residents(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    relationship TEXT NULL,
    age SMALLINT NULL CHECK (age BETWEEN 0 AND 150),
    contact_no TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_family_members_tenant_id ON family_members(tenant_id);
CREATE INDEX idx_family_members_resident_id ON family_members(resident_id);
