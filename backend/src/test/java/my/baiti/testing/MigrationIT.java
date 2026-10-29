package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
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
          .isEqualTo(28);
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

  private static long scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }
}
