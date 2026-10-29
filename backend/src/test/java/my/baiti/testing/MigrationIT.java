package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MigrationIT {

  private static NativePostgresConfig config;

  @BeforeAll
  static void migrateTheVerifiedNativeTarget() {
    config = NativePostgresMigrations.migrateVerifiedTarget();
  }

  @Test
  void schemaContainsAllTwentySixAuthoredRelations() throws Exception {
    try (var connection = config.openMigrationConnection()) {
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_tables
           WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
          """))
          .isEqualTo(26);
      assertThat(scalar(connection, "SELECT count(*) FROM flyway_schema_history WHERE success"))
          .isEqualTo(31);
    }
  }

  @Test
  void everyTenantRelationHasForcedReadAndWriteIsolationPolicies() throws Exception {
    try (var connection = config.openMigrationConnection()) {
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
           WHERE n.nspname = 'public' AND c.relkind = 'r'
             AND EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid = c.oid
                         AND a.attname = 'tenant_id' AND NOT a.attisdropped)
             AND c.relrowsecurity AND c.relforcerowsecurity
             AND EXISTS (SELECT 1 FROM pg_policies p WHERE p.schemaname = n.nspname
                         AND p.tablename = c.relname AND p.policyname = 'tenant_isolation'
                         AND p.qual IS NOT NULL AND p.with_check IS NOT NULL)
          """))
          .isEqualTo(24);
    }
  }

  @Test
  void applicationCannotRewritePaymentOrAuditHistory() throws Exception {
    try (var connection = config.openAppConnection()) {
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM (VALUES ('payments'), ('activity_logs')) AS protected(table_name)
           WHERE has_table_privilege(current_user, table_name, 'SELECT')
             AND has_table_privilege(current_user, table_name, 'INSERT')
             AND NOT has_table_privilege(current_user, table_name, 'UPDATE')
             AND NOT has_table_privilege(current_user, table_name, 'DELETE')
          """))
          .isEqualTo(2);
    }
  }

  @Test
  void tenantSettingReaderIsUnprivilegedAndHasARestrictedSearchPath() throws Exception {
    try (var connection = config.openMigrationConnection()) {
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
           WHERE n.nspname = 'public' AND p.proname = 'baiti_current_tenant'
             AND p.pronargs = 0 AND NOT p.prosecdef AND p.provolatile = 's'
             AND p.prorettype = 'uuid'::regtype
             AND p.proconfig @> ARRAY['search_path=pg_catalog']
             AND pg_get_userbyid(p.proowner) = 'baiti_migrator'
             AND has_function_privilege('baiti_app', p.oid, 'EXECUTE')
             AND has_function_privilege('baiti_migrator', p.oid, 'EXECUTE')
             AND NOT EXISTS (SELECT 1 FROM aclexplode(p.proacl) acl
                             WHERE acl.grantee = 0 AND acl.privilege_type = 'EXECUTE')
          """))
          .isEqualTo(1);
    }
  }

  @Test
  void platformPoliciesNameOnlyTheTrustedOwnerAndExcludeDelete() throws Exception {
    try (var connection = config.openMigrationConnection()) {
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_policies
           WHERE schemaname = 'public'
             AND tablename IN ('tenants', 'super_admins')
             AND policyname IN ('platform_select', 'platform_insert', 'platform_update')
             AND roles = ARRAY['baiti_migrator']::name[]
             AND cmd IN ('SELECT', 'INSERT', 'UPDATE')
             AND (qual = 'true' OR with_check = 'true')
          """))
          .isEqualTo(6);
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_policies
           WHERE schemaname = 'public'
             AND tablename IN ('tenants', 'super_admins')
             AND policyname LIKE 'platform_%' AND cmd IN ('ALL', 'DELETE')
          """))
          .isZero();
      assertThat(
              scalar(
                  connection,
                  """
          SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
           WHERE n.nspname = 'public' AND c.relname IN ('tenants', 'super_admins')
             AND c.relrowsecurity AND c.relforcerowsecurity
          """))
          .isEqualTo(2);
    }
  }

  @Test
  void everyForeignKeyHasAValidUnconditionalLeadingIndex() throws Exception {
    try (var connection = config.openMigrationConnection();
        var statement =
            connection.prepareStatement(
                """
        SELECT t.relname || '.' || c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.contype = 'f' AND n.nspname = 'public'
          AND NOT EXISTS (
            SELECT 1 FROM pg_index i WHERE i.indrelid = c.conrelid
              AND i.indisvalid AND i.indisready AND i.indpred IS NULL
              AND i.indnkeyatts >= cardinality(c.conkey)
              AND NOT EXISTS (
                SELECT 1 FROM unnest(c.conkey) AS fk(attnum)
                WHERE NOT EXISTS (
                  SELECT 1 FROM unnest(i.indkey::smallint[])
                    WITH ORDINALITY AS k(attnum, ord)
                  WHERE k.ord <= cardinality(c.conkey) AND k.attnum = fk.attnum
                )
              )
          )
        ORDER BY 1
        """);
        var result = statement.executeQuery()) {
      var missing = new ArrayList<String>();
      while (result.next()) {
        missing.add(result.getString(1));
      }
      assertThat(missing).isEmpty();
    }
  }

  private static long scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }
}
