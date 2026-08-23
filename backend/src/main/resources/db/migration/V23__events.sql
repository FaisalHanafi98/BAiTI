-- 04_DATABASE_DESIGN.md §4.7 - V23
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    title TEXT NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    location TEXT NULL,
    description TEXT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID NULL,
    CONSTRAINT chk_events_date_order CHECK (end_date >= start_date)
);

CREATE INDEX idx_events_tenant_id ON events(tenant_id);
CREATE INDEX idx_events_tenant_start ON events(tenant_id, start_date);
