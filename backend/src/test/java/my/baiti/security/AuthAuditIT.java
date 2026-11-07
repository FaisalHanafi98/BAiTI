package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import my.baiti.tenancy.TenantContext;
import my.baiti.testing.AuthenticationFixture;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "audit-test"})
class AuthAuditIT extends NativePostgresTestSupport {
  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  private static final UUID B = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");
  private static final String PASSWORD = "synthetic audit phrase";
  @Autowired private MockMvc mvc;
  @Autowired private DataSource source;

  @Autowired
  @Qualifier("platformJdbcTemplate")
  private JdbcTemplate platform;

  @Autowired
  @Qualifier("tenantAuditExecutor")
  private ThreadPoolTaskExecutor auditExecutor;

  @Test
  void asyncLoginWritesStayWithTheOriginatingTenant() throws Exception {
    String addressA = address();
    String addressB = address();
    String failedAddress = address();
    try (var first = new AuthenticationFixture(source, A, PASSWORD);
        var second = new AuthenticationFixture(source, B, PASSWORD)) {
      signIn(first, "baiti-foundation-a", addressA, PASSWORD).andExpect(status().isOk());
      signIn(second, "baiti-foundation-b", addressB, PASSWORD).andExpect(status().isOk());
      signIn(first, "baiti-foundation-a", failedAddress, "incorrect")
          .andExpect(status().isUnauthorized());
      assertThat(workerContextCleared()).isTrue();
      assertThat(actors(first, addressA, "LOGIN_SUCCESS")).containsExactly(first.userId());
      assertThat(actors(second, addressB, "LOGIN_SUCCESS")).containsExactly(second.userId());
      assertThat(actors(first, addressB, "LOGIN_SUCCESS")).isEmpty();
      assertThat(actors(second, addressA, "LOGIN_SUCCESS")).isEmpty();
      assertThat(actors(first, failedAddress, "LOGIN_FAILED")).containsExactly((UUID) null);
      assertThat(actors(second, failedAddress, "LOGIN_FAILED")).isEmpty();
    } finally {
      workerContextCleared();
      clean(A, addressA);
      clean(B, addressB);
      clean(A, failedAddress);
    }
  }

  private boolean workerContextCleared() throws Exception {
    return auditExecutor.submit(() -> TenantContext.current().isEmpty()).get(10, TimeUnit.SECONDS);
  }

  private ResultActions signIn(
      AuthenticationFixture fixture, String slug, String address, String password)
      throws Exception {
    return mvc.perform(
        post("/api/auth/login")
            .with(
                request -> {
                  request.setRemoteAddr(address);
                  return request;
                })
            .header("X-Tenant-Slug", slug)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + fixture.email() + "\",\"password\":\"" + password + "\"}"));
  }

  private List<UUID> actors(AuthenticationFixture fixture, String address, String action) {
    var result = new AtomicReference<List<UUID>>();
    fixture.scoped(
        jdbc ->
            result.set(
                jdbc.queryForList(
                    "SELECT user_id FROM activity_logs WHERE action = ? AND ip_address = CAST(? AS inet)",
                    UUID.class,
                    action,
                    address)));
    return result.get();
  }

  private void clean(UUID tenant, String address) {
    new TransactionTemplate(new JdbcTransactionManager(platform.getDataSource()))
        .executeWithoutResult(
            status -> {
              platform.queryForObject(
                  "SELECT set_config('app.current_tenant', ?, true)",
                  String.class,
                  tenant.toString());
              platform.update(
                  "DELETE FROM activity_logs WHERE tenant_id = ? AND ip_address = CAST(? AS inet)",
                  tenant,
                  address);
            });
  }

  private static String address() {
    var bits = UUID.randomUUID().toString().replace("-", "");
    var address = new StringBuilder("2001:db8");
    for (int index = 0; index < 24; index += 4) {
      address.append(':').append(bits, index, index + 4);
    }
    return address.toString();
  }
}
