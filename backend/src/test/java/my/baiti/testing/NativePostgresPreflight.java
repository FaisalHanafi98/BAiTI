package my.baiti.testing;

import java.sql.Connection;
import java.sql.SQLException;

final class NativePostgresPreflight {

  private static final String IDENTITY_QUERY =
      """
      SELECT current_database() AS database_name,
             current_setting('server_version_num')::integer AS server_version,
             pg_get_userbyid(d.datdba) AS database_owner,
             pg_get_userbyid(n.nspowner) AS schema_owner,
             shobj_description(d.oid, 'pg_database') AS marker,
             current_user AS session_user,
             (r.rolsuper OR r.rolbypassrls OR r.rolcreaterole
               OR r.rolcreatedb OR r.rolreplication) AS privileged,
             EXISTS (SELECT 1 FROM pg_roles other_role
                     WHERE other_role.oid <> r.oid
                       AND other_role.rolname <> 'pg_database_owner'
                       AND pg_has_role(r.oid, other_role.oid, 'MEMBER')) AS other_membership,
             (EXISTS (SELECT 1 FROM pg_namespace owned_schema WHERE owned_schema.nspowner = r.oid)
               OR EXISTS (SELECT 1 FROM pg_class owned_relation WHERE owned_relation.relowner = r.oid)) AS owns_objects
        FROM pg_database d
        JOIN pg_namespace n ON n.nspname = 'public'
        JOIN pg_roles r ON r.rolname = current_user
       WHERE d.datname = current_database()
      """;

  static void verify(NativePostgresConfig config) {
    try (var migration = config.openMigrationConnection();
        var app = config.openAppConnection()) {
      var migrationIdentity = observe(migration);
      var appIdentity = observe(app);
      migrationIdentity
          .withRoles(migrationIdentity, appIdentity)
          .requireSafe(config.databaseName(), config.ownershipMarker());
      appIdentity
          .withRoles(migrationIdentity, appIdentity)
          .requireSafe(config.databaseName(), config.ownershipMarker());
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Native PG16 preflight failed (SQL state "
              + e.getSQLState()
              + "); verify the dedicated test server, database and role credentials");
    }
  }

  private static Observation observe(Connection connection) throws SQLException {
    connection.setReadOnly(true);
    try (var statement = connection.prepareStatement(IDENTITY_QUERY)) {
      statement.setQueryTimeout(5);
      try (var result = statement.executeQuery()) {
        if (!result.next()) {
          throw new IllegalStateException("Native test database catalog identity is unavailable");
        }
        return new Observation(
            result.getString("database_name"),
            result.getInt("server_version"),
            result.getString("database_owner"),
            result.getString("schema_owner"),
            result.getString("marker"),
            result.getString("session_user"),
            result.getBoolean("privileged"),
            result.getBoolean("other_membership"),
            result.getBoolean("owns_objects"));
      }
    }
  }

  private record Observation(
      String database,
      int version,
      String databaseOwner,
      String schemaOwner,
      String marker,
      String user,
      boolean privileged,
      boolean otherMembership,
      boolean ownsObjects) {

    NativePostgresIdentity withRoles(Observation migrator, Observation app) {
      return new NativePostgresIdentity(
          database,
          version,
          databaseOwner,
          schemaOwner,
          marker,
          migrator.user,
          migrator.privileged || migrator.otherMembership,
          app.user,
          app.privileged,
          app.otherMembership,
          app.ownsObjects);
    }
  }
}
