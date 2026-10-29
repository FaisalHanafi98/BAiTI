-- 04_DATABASE_DESIGN.md section 5: cleared/invalid tenant context must deny without UUID-cast errors.
CREATE OR REPLACE FUNCTION public.baiti_current_tenant()
RETURNS UUID
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = pg_catalog
AS $function$
DECLARE
    tenant_setting TEXT;
BEGIN
    tenant_setting := current_setting('app.current_tenant', true);
    IF tenant_setting IS NULL OR length(tenant_setting) <> 36 THEN
        RETURN NULL;
    END IF;
    RETURN tenant_setting::UUID;
EXCEPTION WHEN invalid_text_representation THEN
    RETURN NULL;
END;
$function$;

REVOKE ALL ON FUNCTION public.baiti_current_tenant() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.baiti_current_tenant() TO baiti_app, baiti_migrator;

DO $policies$
DECLARE
    tenant_table TEXT;
BEGIN
    FOREACH tenant_table IN ARRAY ARRAY[
        'permissions', 'roles', 'roles_permissions', 'users', 'refresh_tokens',
        'blocks', 'units', 'residents', 'family_members', 'payment_methods',
        'bills', 'payments', 'complaint_categories', 'complaints', 'complaint_updates',
        'maintenance_requests', 'facilities', 'facility_bookings', 'announcements',
        'events', 'visitor_pre_registrations', 'visitors', 'notifications', 'activity_logs'
    ]
    LOOP
        EXECUTE format(
            'ALTER POLICY tenant_isolation ON public.%I '
            'USING (tenant_id = public.baiti_current_tenant()) '
            'WITH CHECK (tenant_id = public.baiti_current_tenant())',
            tenant_table
        );
    END LOOP;
END;
$policies$;

ALTER POLICY tenant_self_select ON public.tenants
    USING (id = public.baiti_current_tenant());
