-- 04_DATABASE_DESIGN.md §4.5 - V14
CREATE TABLE bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    unit_id UUID NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
    billing_month DATE NOT NULL CHECK (date_trunc('month', billing_month) = billing_month),
    bill_type TEXT NOT NULL DEFAULT 'MAINTENANCE_FEE',
    description TEXT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'MYR',
    status TEXT NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    due_date DATE NOT NULL,
    cancelled_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL,
    CONSTRAINT uq_bills_unit_month_type UNIQUE (tenant_id, unit_id, billing_month, bill_type)
);

CREATE INDEX idx_bills_tenant_id ON bills(tenant_id);
CREATE INDEX idx_bills_unit_id ON bills(unit_id);
CREATE INDEX idx_bills_tenant_status ON bills(tenant_id, status);
CREATE INDEX idx_bills_tenant_unpaid_due ON bills(tenant_id, due_date) WHERE status = 'UNPAID';
