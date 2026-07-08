package my.baiti.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Emits one DEBUG line at startup proving the runtime pool connects as {@code baiti_app} (the
 * non-owner, NOBYPASSRLS role that RLS binds to). Supports the P02 DoD: "the log line proves the
 * runtime pool user is baiti_app". Also surfaces the tenant context setting, which is empty until
 * P05 wires {@code TenantAwareDataSource}.
 */
@Component
public class StartupDiagnostics implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

  private final JdbcTemplate jdbc;

  public StartupDiagnostics(DataSource appDataSource) {
    this.jdbc = new JdbcTemplate(appDataSource);
  }

  @Override
  public void run(ApplicationArguments args) {
    String currentUser = jdbc.queryForObject("SELECT current_user", String.class);
    String tenantContext =
        jdbc.queryForObject("SELECT current_setting('app.current_tenant', true)", String.class);
    log.debug(
        "BAiTI startup diagnostics — pool current_user={} (NOBYPASSRLS, RLS binds here);"
            + " app.current_tenant={} (empty until P05 TenantAwareDataSource)",
        currentUser,
        tenantContext == null ? "<unset>" : tenantContext);
  }
}
