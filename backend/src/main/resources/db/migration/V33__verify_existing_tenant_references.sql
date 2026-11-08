DO $verify$
DECLARE
    tenant UUID;
    link RECORD;
    invalid BOOLEAN;
BEGIN
    IF public.baiti_current_tenant() IS NOT NULL THEN
        RAISE EXCEPTION 'Tenant reference verification requires an unscoped migration connection';
    END IF;

    FOR tenant IN SELECT id FROM public.tenants ORDER BY id LOOP
        PERFORM set_config('app.current_tenant', tenant::text, true);

        FOR link IN
            SELECT fk.conname, child.relname AS child_table, parent.relname AS parent_table,
                   reference.attname AS child_column
            FROM pg_constraint fk
            JOIN pg_class child ON child.oid = fk.conrelid
            JOIN pg_class parent ON parent.oid = fk.confrelid
            JOIN pg_attribute child_tenant
              ON child_tenant.attrelid = child.oid AND child_tenant.attnum = fk.conkey[1]
            JOIN pg_attribute parent_tenant
              ON parent_tenant.attrelid = parent.oid AND parent_tenant.attnum = fk.confkey[1]
            JOIN pg_attribute reference
              ON reference.attrelid = child.oid AND reference.attnum = fk.conkey[2]
            JOIN pg_attribute parent_key
              ON parent_key.attrelid = parent.oid AND parent_key.attnum = fk.confkey[2]
            WHERE fk.contype = 'f' AND cardinality(fk.conkey) = 2
              AND child.relnamespace = 'public'::regnamespace
              AND parent.relnamespace = 'public'::regnamespace
              AND child_tenant.attname = 'tenant_id' AND parent_tenant.attname = 'tenant_id'
              AND parent_key.attname = 'id'
            ORDER BY child.relname, fk.conname
        LOOP
            EXECUTE format(
                'SELECT EXISTS (
                    SELECT 1 FROM public.%I child
                    WHERE child.tenant_id = $1 AND child.%I IS NOT NULL
                      AND NOT EXISTS (
                        SELECT 1 FROM public.%I parent
                        WHERE parent.tenant_id = child.tenant_id AND parent.id = child.%I
                      )
                )', link.child_table, link.child_column, link.parent_table, link.child_column
            ) INTO invalid USING tenant;
            IF invalid THEN
                RAISE EXCEPTION USING
                    ERRCODE = '23503',
                    MESSAGE = format('Existing tenant reference violates %I', link.conname),
                    CONSTRAINT = link.conname,
                    TABLE = link.child_table,
                    SCHEMA = 'public';
            END IF;
        END LOOP;
    END LOOP;

    PERFORM set_config('app.current_tenant', '', true);
END;
$verify$;
