package my.baiti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import my.baiti.testing.FoundationTenantConfiguration;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FoundationTenantConfiguration.class)
class HealthIT extends NativePostgresTestSupport {

  private final TestRestTemplate rest;
  private final DataSource dataSource;
  private final MockMvc mvc;

  @Autowired
  HealthIT(TestRestTemplate rest, DataSource dataSource, MockMvc mvc) {
    this.rest = rest;
    this.dataSource = dataSource;
    this.mvc = mvc;
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

  @Test
  @WithMockUser(username = "synthetic-documentation-reviewer")
  void authenticatedApiDocumentationUsesTheCurrentOpenApiStack() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").isString())
        .andExpect(jsonPath("$.info").isMap())
        .andExpect(jsonPath("$.paths").isMap());
  }

  @Test
  void apiDocumentationRemainsProtectedFromAnonymousRequests() throws Exception {
    var response = mvc.perform(get("/v3/api-docs")).andReturn().getResponse();
    assertThat(response.getStatus()).isIn(401, 403);
  }
}
