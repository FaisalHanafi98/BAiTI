-- 04_DATABASE_DESIGN.md §4.6 - V17
CREATE TABLE complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    resident_id UUID NOT NULL REFERENCES residents(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL REFERENCES complaint_categories(id) ON DELETE RESTRICT,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_complaints_tenant_id ON complaints(tenant_id);
CREATE INDEX idx_complaints_resident_id ON complaints(resident_id);
CREATE INDEX idx_complaints_category_id ON complaints(category_id);
CREATE INDEX idx_complaints_tenant_status ON complaints(tenant_id, status);
