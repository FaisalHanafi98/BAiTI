package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextFilterTest {

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void requestReceivesOnlyTheResolvedTenantAndClearsItAfterward() throws Exception {
    var tenant = UUID.randomUUID();
    TenantContext.set(UUID.randomUUID());
    var filter = new TenantContextFilter(request -> Optional.of(tenant));
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/units"),
        new MockHttpServletResponse(),
        (request, response) -> assertThat(TenantContext.current()).contains(tenant));
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void downstreamFailureStillClearsTheTenant() {
    var filter = new TenantContextFilter(request -> Optional.of(UUID.randomUUID()));
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    new MockHttpServletRequest("GET", "/api/units"),
                    new MockHttpServletResponse(),
                    (request, response) -> {
                      throw new IllegalStateException("Synthetic downstream failure");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void invalidResolutionReturnsGeneric401WithoutCallingTheApplication() throws Exception {
    TenantContext.set(UUID.randomUUID());
    var filter =
        new TenantContextFilter(
            request -> {
              throw new TenantResolutionException();
            });
    var response = new MockHttpServletResponse();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/api/units"),
        response,
        (request, result) -> {
          throw new AssertionError("Application must not be called");
        });
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Authentication failed\"}");
    assertThat(TenantContext.current()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/admin",
        "/api/admin/auth/login",
        "/api/admin/tenants",
        "/actuator/health",
        "/actuator/health/readiness"
      })
  void platformAndHealthRequestsAlwaysHaveEmptyContext(String path) throws Exception {
    TenantContext.set(UUID.randomUUID());
    var filter =
        new TenantContextFilter(
            request -> {
              throw new AssertionError("Tenant resolution must not be called");
            });
    filter.doFilter(
        new MockHttpServletRequest("GET", path),
        new MockHttpServletResponse(),
        (request, response) -> assertThat(TenantContext.current()).isEmpty());
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void missingContextCannotReuseAStaleThreadValue() throws Exception {
    TenantContext.set(UUID.randomUUID());
    new TenantContextFilter(request -> Optional.empty())
        .doFilter(
            new MockHttpServletRequest("GET", "/api/units"),
            new MockHttpServletResponse(),
            (request, response) -> assertThat(TenantContext.current()).isEmpty());
    assertThat(TenantContext.current()).isEmpty();
  }
}
