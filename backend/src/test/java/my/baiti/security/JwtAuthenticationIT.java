package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import my.baiti.tenancy.TenantContext;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationIT extends NativePostgresTestSupport {

  private static final UUID A = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");
  @Autowired private MockMvc mvc;
  @Autowired private JwtService service;

  @Test
  void matchingSignedTenantTokenCanAccessAProtectedRoute() throws Exception {
    mvc.perform(
            get("/v3/api-docs")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token(A)))
        .andExpect(status().isOk());
  }

  @Test
  void signedTenantBTokenCannotUseTenantASlug() throws Exception {
    var token = token(UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93"));
    mvc.perform(
            get("/v3/api-docs")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
    assertThat(TenantContext.current()).isEmpty();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void signedTokenNeedsAResolvedTenant() throws Exception {
    mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token(A)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void malformedExpiredAndDuplicateAuthorizationValuesAreRejected() throws Exception {
    var expired =
        new JwtService(jwtTestSecret(), Clock.offset(Clock.systemUTC(), Duration.ofMinutes(-16)))
            .issue(
                new TenantPrincipal(UUID.randomUUID(), A, Set.of("Admin"), Set.of("BILL_MANAGE")));
    for (String header :
        new String[] {"Bearer malformed", "Bearer " + expired, "Basic synthetic-unsupported"}) {
      mvc.perform(
              get("/v3/api-docs")
                  .header("X-Tenant-Slug", "baiti-foundation-a")
                  .header("Authorization", header))
          .andExpect(status().isUnauthorized());
    }
    String token = token(A);
    mvc.perform(
            get("/v3/api-docs")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void validTenantTokenCannotReachPlatformRoutes() throws Exception {
    mvc.perform(
            get("/api/admin/tenants")
                .header("X-Tenant-Slug", "baiti-foundation-a")
                .header("Authorization", "Bearer " + token(A)))
        .andExpect(status().isForbidden());
  }

  @Test
  void authenticatedRequestDoesNotCreateASessionOrAuthenticateTheNextRequest() throws Exception {
    var result =
        mvc.perform(
                get("/v3/api-docs")
                    .header("X-Tenant-Slug", "baiti-foundation-a")
                    .header("Authorization", "Bearer " + token(A)))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(result.getRequest().getSession(false)).isNull();
    assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    mvc.perform(get("/v3/api-docs").header("X-Tenant-Slug", "baiti-foundation-a"))
        .andExpect(status().isUnauthorized());
  }

  private String token(UUID tenant) {
    return service.issue(
        new TenantPrincipal(UUID.randomUUID(), tenant, Set.of("Admin"), Set.of("BILL_MANAGE")));
  }
}
