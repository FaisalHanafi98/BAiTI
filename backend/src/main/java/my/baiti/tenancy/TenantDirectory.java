package my.baiti.tenancy;

import java.util.Optional;
import java.util.UUID;
import my.baiti.tenancy.admin.UsesPrivilegedDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@UsesPrivilegedDataSource
public class TenantDirectory implements TenantLookup {

  private final JdbcTemplate jdbc;

  public TenantDirectory(@Qualifier("platformJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> findActiveTenant(String slug) {
    return jdbc
        .query(
            "SELECT id FROM tenants WHERE slug = ? AND status = 'ACTIVE'",
            (row, index) -> row.getObject(1, UUID.class),
            slug)
        .stream()
        .findFirst();
  }
}
