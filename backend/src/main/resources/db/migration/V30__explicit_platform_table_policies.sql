-- 04_DATABASE_DESIGN.md section 5 / ADR 0003: the trusted owner remains subject to FORCE RLS.
CREATE POLICY platform_select ON public.tenants
    FOR SELECT TO baiti_migrator
    USING (true);

CREATE POLICY platform_insert ON public.tenants
    FOR INSERT TO baiti_migrator
    WITH CHECK (true);

CREATE POLICY platform_update ON public.tenants
    FOR UPDATE TO baiti_migrator
    USING (true)
    WITH CHECK (true);

CREATE POLICY platform_select ON public.super_admins
    FOR SELECT TO baiti_migrator
    USING (true);

CREATE POLICY platform_insert ON public.super_admins
    FOR INSERT TO baiti_migrator
    WITH CHECK (true);

CREATE POLICY platform_update ON public.super_admins
    FOR UPDATE TO baiti_migrator
    USING (true)
    WITH CHECK (true);
