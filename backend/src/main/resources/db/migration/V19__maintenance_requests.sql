-- 04_DATABASE_DESIGN.md §4.6 - V19
CREATE TABLE maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    description TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_maintenance_requests_tenant_id ON maintenance_requests(tenant_id);
CREATE INDEX idx_maintenance_requests_unit_id ON maintenance_requests(unit_id);
CREATE INDEX idx_maintenance_requests_tenant_status_priority ON maintenance_requests(tenant_id, status, priority);
