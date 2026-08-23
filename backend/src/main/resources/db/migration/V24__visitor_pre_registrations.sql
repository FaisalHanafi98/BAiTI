-- 04_DATABASE_DESIGN.md §4.7 - V24
CREATE TABLE visitor_pre_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    resident_id UUID NOT NULL REFERENCES residents(id) ON DELETE RESTRICT,
    visitor_name TEXT NOT NULL,
    visit_date DATE NOT NULL,
    approval_code TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'USED', 'EXPIRED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_visitor_pre_reg_approval_code UNIQUE (tenant_id, approval_code)
);

CREATE INDEX idx_visitor_pre_reg_tenant_id ON visitor_pre_registrations(tenant_id);
CREATE INDEX idx_visitor_pre_reg_resident_id ON visitor_pre_registrations(resident_id);
CREATE INDEX idx_visitor_pre_reg_tenant_visit ON visitor_pre_registrations(tenant_id, visit_date);
