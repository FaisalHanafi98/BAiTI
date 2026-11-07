package my.baiti.system;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantActivityWriter {
  private final JdbcTemplate app;

  public TenantActivityWriter(@Qualifier("appJdbcTemplate") JdbcTemplate app) {
    this.app = app;
  }

  public void append(TenantActivity activity) {
    var occurred = OffsetDateTime.ofInstant(activity.occurredAt(), ZoneOffset.UTC);
    app.update(
        "INSERT INTO activity_logs (tenant_id, user_id, action, ip_address, created_at, updated_at)"
            + " VALUES (?, ?, ?, CAST(? AS inet), ?, ?)",
        activity.tenantId(),
        activity.actorId(),
        activity.action().name(),
        activity.address(),
        occurred,
        occurred);
  }
}
