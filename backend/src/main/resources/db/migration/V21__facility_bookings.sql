-- 04_DATABASE_DESIGN.md §4.7 - V21
CREATE TABLE facility_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE RESTRICT,
    resident_id UUID NOT NULL REFERENCES residents(id) ON DELETE RESTRICT,
    booking_date DATE NOT NULL,
    time_slot TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by UUID NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NULL,
    updated_by UUID NULL
);

CREATE INDEX idx_facility_bookings_tenant_id ON facility_bookings(tenant_id);
CREATE INDEX idx_facility_bookings_facility_id ON facility_bookings(facility_id);
CREATE INDEX idx_facility_bookings_resident_id ON facility_bookings(resident_id);
CREATE UNIQUE INDEX uq_facility_bookings_slot ON facility_bookings(tenant_id, facility_id, booking_date, time_slot) WHERE status IN ('PENDING', 'APPROVED');
