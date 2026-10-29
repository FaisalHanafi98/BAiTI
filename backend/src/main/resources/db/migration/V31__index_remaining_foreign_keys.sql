-- 04_DATABASE_DESIGN.md sections 1.10 and 5: complete the existing foreign-key index contract.
CREATE INDEX idx_announcements_posted_by ON public.announcements(posted_by);
CREATE INDEX idx_complaints_unit_id ON public.complaints(unit_id);
CREATE INDEX idx_events_created_by ON public.events(created_by);
CREATE INDEX idx_facility_bookings_approved_by ON public.facility_bookings(approved_by);
CREATE INDEX idx_maintenance_requests_requested_by ON public.maintenance_requests(requested_by);
CREATE INDEX idx_visitors_pre_registration_id ON public.visitors(pre_registration_id);
