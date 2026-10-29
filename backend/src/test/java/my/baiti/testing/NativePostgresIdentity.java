package my.baiti.testing;

record NativePostgresIdentity(
    String database,
    int serverVersion,
    String databaseOwner,
    String schemaOwner,
    String marker,
    String migratorUser,
    boolean migratorPrivileged,
    String appUser,
    boolean appPrivileged,
    boolean appHasOtherMembership,
    boolean appOwnsObjects) {

  void requireSafe(String expectedDatabase, String expectedMarker) {
    if (!expectedDatabase.equals(database) || serverVersion / 10000 != 16) {
      throw new IllegalStateException(
          "Native test database identity/version does not match PG16 target");
    }
    if (!"baiti_migrator".equals(databaseOwner) || !"baiti_migrator".equals(schemaOwner)) {
      throw new IllegalStateException(
          "Native test database and public schema must belong to baiti_migrator");
    }
    if (!expectedMarker.equals(marker)) {
      throw new IllegalStateException("Native test database ownership marker does not match");
    }
    if (!"baiti_migrator".equals(migratorUser) || migratorPrivileged) {
      throw new IllegalStateException("Native migration role identity/privileges are unsafe");
    }
    if (!"baiti_app".equals(appUser) || appPrivileged || appHasOtherMembership || appOwnsObjects) {
      throw new IllegalStateException("Native runtime role must be non-owner and unprivileged");
    }
  }

  @Override
  public String toString() {
    return "NativePostgresIdentity[database=" + database + ", serverVersion=" + serverVersion + "]";
  }
}
