-- 04_DATABASE_DESIGN.md §4.7 - V25
CREATE TABLE visitors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    resident_id UUID NOT NULL REFERENCES residents(id) ON DELETE RESTRICT,
    pre_registration_id UUID NULL REFERENCES visitor_pre_registrations(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    vehicle_no TEXT NULL,
    visit_reason TEXT NULL,
    entry_time TIMESTAMPTZ NULL,
    exit_time TIMESTAMPTZ NULL,
    status TEXT NOT NULL DEFAULT 'CHECKED_IN' CHECK (status IN ('CHECKED_IN', 'CHECKED_OUT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT chk_visitors_time_order CHECK (exit_time IS NULL OR (entry_time IS NOT NULL AND exit_time >= entry_time))
);

CREATE INDEX idx_visitors_tenant_id ON visitors(tenant_id);
CREATE INDEX idx_visitors_resident_id ON visitors(resident_id);
CREATE INDEX idx_visitors_tenant_active ON visitors(tenant_id) WHERE exit_time IS NULL;
