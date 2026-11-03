package my.baiti.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Logs runtime identity/context; native boundary tests separately verify role attributes. */
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
        "BAiTI startup diagnostics — pool current_user={};"
            + " app.current_tenant={} (empty at startup)",
        currentUser,
        tenantContext == null ? "<unset>" : tenantContext);
  }
}
