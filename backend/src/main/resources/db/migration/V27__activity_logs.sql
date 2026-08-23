-- 04_DATABASE_DESIGN.md §4.8 - V27
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    user_id UUID NULL,
    action TEXT NOT NULL,
    detail JSONB NULL,
    ip_address INET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_activity_logs_tenant_id ON activity_logs(tenant_id);
CREATE INDEX idx_activity_logs_tenant_created ON activity_logs(tenant_id, created_at DESC);
CREATE INDEX idx_activity_logs_tenant_user ON activity_logs(tenant_id, user_id);
