-- 04_DATABASE_DESIGN.md §4.7 - V22
CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    posted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    attachment TEXT NULL,
    target_audience TEXT NOT NULL DEFAULT 'ALL' CHECK (target_audience IN ('ALL', 'OWNERS', 'TENANTS')),
    posted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_announcements_tenant_id ON announcements(tenant_id);
CREATE INDEX idx_announcements_tenant_posted ON announcements(tenant_id, posted_at DESC);
