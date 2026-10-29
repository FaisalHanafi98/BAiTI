package my.baiti;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import my.baiti.testing.FoundationTenantConfiguration;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FoundationTenantConfiguration.class)
class HealthIT extends NativePostgresTestSupport {

  private final TestRestTemplate rest;
  private final DataSource dataSource;

  @Autowired
  HealthIT(TestRestTemplate rest, DataSource dataSource) {
    this.rest = rest;
    this.dataSource = dataSource;
  }

  @Test
  void runtimePoolUsesTheUnprivilegedApplicationRole() throws Exception {
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "SELECT current_user, rolsuper, rolbypassrls FROM pg_roles WHERE rolname = current_user");
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      assertThat(result.getString("current_user")).isEqualTo("baiti_app");
      assertThat(result.getBoolean("rolsuper")).isFalse();
      assertThat(result.getBoolean("rolbypassrls")).isFalse();
    }
  }

  @Test
  void healthEndpointIsUpAndAnonymous() {
    var response = rest.getForEntity("/actuator/health", String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }
}
