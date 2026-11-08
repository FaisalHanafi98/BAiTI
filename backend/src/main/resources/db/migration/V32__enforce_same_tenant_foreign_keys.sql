ALTER TABLE public.bills ADD CONSTRAINT uq_bills_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.blocks ADD CONSTRAINT uq_blocks_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.complaint_categories ADD CONSTRAINT uq_complaint_categories_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.complaints ADD CONSTRAINT uq_complaints_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.facilities ADD CONSTRAINT uq_facilities_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.payment_methods ADD CONSTRAINT uq_payment_methods_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.permissions ADD CONSTRAINT uq_permissions_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.residents ADD CONSTRAINT uq_residents_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.roles ADD CONSTRAINT uq_roles_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.units ADD CONSTRAINT uq_units_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.users ADD CONSTRAINT uq_users_tenant_id_id UNIQUE (tenant_id, id);
ALTER TABLE public.visitor_pre_registrations ADD CONSTRAINT uq_visitor_pre_registrations_tenant_id_id UNIQUE (tenant_id, id);

CREATE INDEX idx_roles_permissions_tenant_role_id ON public.roles_permissions (tenant_id, role_id);
CREATE INDEX idx_roles_permissions_tenant_permission_id ON public.roles_permissions (tenant_id, permission_id);
CREATE INDEX idx_users_tenant_role_id ON public.users (tenant_id, role_id);
CREATE INDEX idx_refresh_tokens_tenant_user_id ON public.refresh_tokens (tenant_id, user_id);
CREATE INDEX idx_residents_tenant_user_id ON public.residents (tenant_id, user_id);
CREATE INDEX idx_residents_tenant_unit_id ON public.residents (tenant_id, unit_id);
CREATE INDEX idx_family_members_tenant_resident_id ON public.family_members (tenant_id, resident_id);
CREATE INDEX idx_payments_tenant_bill_id ON public.payments (tenant_id, bill_id);
CREATE INDEX idx_payments_tenant_resident_id ON public.payments (tenant_id, resident_id);
CREATE INDEX idx_payments_tenant_payment_method_id ON public.payments (tenant_id, payment_method_id);
CREATE INDEX idx_complaints_tenant_resident_id ON public.complaints (tenant_id, resident_id);
CREATE INDEX idx_complaints_tenant_unit_id ON public.complaints (tenant_id, unit_id);
CREATE INDEX idx_complaints_tenant_category_id ON public.complaints (tenant_id, category_id);
CREATE INDEX idx_complaint_updates_tenant_complaint_id ON public.complaint_updates (tenant_id, complaint_id);
CREATE INDEX idx_complaint_updates_tenant_user_id ON public.complaint_updates (tenant_id, user_id);
CREATE INDEX idx_maintenance_requests_tenant_unit_id ON public.maintenance_requests (tenant_id, unit_id);
CREATE INDEX idx_maintenance_requests_tenant_requested_by ON public.maintenance_requests (tenant_id, requested_by);
CREATE INDEX idx_facility_bookings_tenant_facility_id ON public.facility_bookings (tenant_id, facility_id);
CREATE INDEX idx_facility_bookings_tenant_resident_id ON public.facility_bookings (tenant_id, resident_id);
CREATE INDEX idx_facility_bookings_tenant_approved_by ON public.facility_bookings (tenant_id, approved_by);
CREATE INDEX idx_announcements_tenant_posted_by ON public.announcements (tenant_id, posted_by);
CREATE INDEX idx_events_tenant_created_by ON public.events (tenant_id, created_by);
CREATE INDEX idx_visitor_pre_registrations_tenant_resident_id ON public.visitor_pre_registrations (tenant_id, resident_id);
CREATE INDEX idx_visitors_tenant_resident_id ON public.visitors (tenant_id, resident_id);
CREATE INDEX idx_visitors_tenant_pre_registration_id ON public.visitors (tenant_id, pre_registration_id);
CREATE INDEX idx_notifications_tenant_user_id ON public.notifications (tenant_id, user_id);

ALTER TABLE public.roles_permissions
    DROP CONSTRAINT roles_permissions_role_id_fkey,
    ADD CONSTRAINT roles_permissions_role_id_fkey
        FOREIGN KEY (tenant_id, role_id) REFERENCES public.roles (tenant_id, id)
        ON DELETE CASCADE,
    DROP CONSTRAINT roles_permissions_permission_id_fkey,
    ADD CONSTRAINT roles_permissions_permission_id_fkey
        FOREIGN KEY (tenant_id, permission_id) REFERENCES public.permissions (tenant_id, id)
        ON DELETE CASCADE;

ALTER TABLE public.users
    DROP CONSTRAINT users_role_id_fkey,
    ADD CONSTRAINT users_role_id_fkey
        FOREIGN KEY (tenant_id, role_id) REFERENCES public.roles (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.refresh_tokens
    DROP CONSTRAINT refresh_tokens_user_id_fkey,
    ADD CONSTRAINT refresh_tokens_user_id_fkey
        FOREIGN KEY (tenant_id, user_id) REFERENCES public.users (tenant_id, id)
        ON DELETE CASCADE;

ALTER TABLE public.units
    DROP CONSTRAINT units_block_id_fkey,
    ADD CONSTRAINT units_block_id_fkey
        FOREIGN KEY (tenant_id, block_id) REFERENCES public.blocks (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.residents
    DROP CONSTRAINT residents_user_id_fkey,
    ADD CONSTRAINT residents_user_id_fkey
        FOREIGN KEY (tenant_id, user_id) REFERENCES public.users (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT residents_unit_id_fkey,
    ADD CONSTRAINT residents_unit_id_fkey
        FOREIGN KEY (tenant_id, unit_id) REFERENCES public.units (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.family_members
    DROP CONSTRAINT family_members_resident_id_fkey,
    ADD CONSTRAINT family_members_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE CASCADE;

ALTER TABLE public.bills
    DROP CONSTRAINT bills_unit_id_fkey,
    ADD CONSTRAINT bills_unit_id_fkey
        FOREIGN KEY (tenant_id, unit_id) REFERENCES public.units (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.payments
    DROP CONSTRAINT payments_bill_id_fkey,
    ADD CONSTRAINT payments_bill_id_fkey
        FOREIGN KEY (tenant_id, bill_id) REFERENCES public.bills (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT payments_resident_id_fkey,
    ADD CONSTRAINT payments_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT payments_payment_method_id_fkey,
    ADD CONSTRAINT payments_payment_method_id_fkey
        FOREIGN KEY (tenant_id, payment_method_id) REFERENCES public.payment_methods (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.complaints
    DROP CONSTRAINT complaints_resident_id_fkey,
    ADD CONSTRAINT complaints_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT complaints_unit_id_fkey,
    ADD CONSTRAINT complaints_unit_id_fkey
        FOREIGN KEY (tenant_id, unit_id) REFERENCES public.units (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT complaints_category_id_fkey,
    ADD CONSTRAINT complaints_category_id_fkey
        FOREIGN KEY (tenant_id, category_id) REFERENCES public.complaint_categories (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.complaint_updates
    DROP CONSTRAINT complaint_updates_complaint_id_fkey,
    ADD CONSTRAINT complaint_updates_complaint_id_fkey
        FOREIGN KEY (tenant_id, complaint_id) REFERENCES public.complaints (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT complaint_updates_user_id_fkey,
    ADD CONSTRAINT complaint_updates_user_id_fkey
        FOREIGN KEY (tenant_id, user_id) REFERENCES public.users (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.maintenance_requests
    DROP CONSTRAINT maintenance_requests_unit_id_fkey,
    ADD CONSTRAINT maintenance_requests_unit_id_fkey
        FOREIGN KEY (tenant_id, unit_id) REFERENCES public.units (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT maintenance_requests_requested_by_fkey,
    ADD CONSTRAINT maintenance_requests_requested_by_fkey
        FOREIGN KEY (tenant_id, requested_by) REFERENCES public.users (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.facility_bookings
    DROP CONSTRAINT facility_bookings_facility_id_fkey,
    ADD CONSTRAINT facility_bookings_facility_id_fkey
        FOREIGN KEY (tenant_id, facility_id) REFERENCES public.facilities (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT facility_bookings_resident_id_fkey,
    ADD CONSTRAINT facility_bookings_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT facility_bookings_approved_by_fkey,
    ADD CONSTRAINT facility_bookings_approved_by_fkey
        FOREIGN KEY (tenant_id, approved_by) REFERENCES public.users (tenant_id, id)
        ON DELETE SET NULL (approved_by);

ALTER TABLE public.announcements
    DROP CONSTRAINT announcements_posted_by_fkey,
    ADD CONSTRAINT announcements_posted_by_fkey
        FOREIGN KEY (tenant_id, posted_by) REFERENCES public.users (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.events
    DROP CONSTRAINT events_created_by_fkey,
    ADD CONSTRAINT events_created_by_fkey
        FOREIGN KEY (tenant_id, created_by) REFERENCES public.users (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.visitor_pre_registrations
    DROP CONSTRAINT visitor_pre_registrations_resident_id_fkey,
    ADD CONSTRAINT visitor_pre_registrations_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE RESTRICT;

ALTER TABLE public.visitors
    DROP CONSTRAINT visitors_resident_id_fkey,
    ADD CONSTRAINT visitors_resident_id_fkey
        FOREIGN KEY (tenant_id, resident_id) REFERENCES public.residents (tenant_id, id)
        ON DELETE RESTRICT,
    DROP CONSTRAINT visitors_pre_registration_id_fkey,
    ADD CONSTRAINT visitors_pre_registration_id_fkey
        FOREIGN KEY (tenant_id, pre_registration_id) REFERENCES public.visitor_pre_registrations (tenant_id, id)
        ON DELETE SET NULL (pre_registration_id);

ALTER TABLE public.notifications
    DROP CONSTRAINT notifications_user_id_fkey,
    ADD CONSTRAINT notifications_user_id_fkey
        FOREIGN KEY (tenant_id, user_id) REFERENCES public.users (tenant_id, id)
        ON DELETE CASCADE;
