package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class TenantResolverTest {

  private static final UUID TENANT = UUID.randomUUID();
  private final TenantLookup lookup =
      slug -> slug.equals("synthetic-community") ? Optional.of(TENANT) : Optional.empty();

  @Test
  void headerResolvesAnActiveSlugAndLeavesMissingContextEmpty() {
    var request = new MockHttpServletRequest();
    var resolver = new HeaderTenantResolver(lookup);
    assertThat(resolver.resolve(request)).isEmpty();
    request.addHeader("X-Tenant-Slug", "synthetic-community");
    assertThat(resolver.resolve(request)).contains(TENANT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "ab",
        "synthetic unknown",
        "' OR true --",
        "synthetic-unknown",
        "SYNTHETIC-COMMUNITY",
        "synthetic-community,another"
      })
  void malformedAndUnknownHeaderValuesFailGenerically(String slug) {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Tenant-Slug", slug);
    assertThatThrownBy(() -> new HeaderTenantResolver(lookup).resolve(request))
        .isInstanceOf(TenantResolutionException.class)
        .hasMessage("Authentication failed");
  }

  @Test
  void duplicateTenantHeadersAreRejected() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Tenant-Slug", "synthetic-community");
    request.addHeader("X-Tenant-Slug", "synthetic-community");
    assertThatThrownBy(() -> new HeaderTenantResolver(lookup).resolve(request))
        .isInstanceOf(TenantResolutionException.class);
  }

  @Test
  void productionResolutionUsesTheHostAndIgnoresTenantAndForwardedHeaders() {
    var request = new MockHttpServletRequest();
    request.setServerName("SYNTHETIC-COMMUNITY.EXAMPLE.INVALID");
    request.addHeader("X-Tenant-Slug", "synthetic-other");
    request.addHeader("X-Forwarded-Host", "synthetic-other.example.invalid");
    var resolver = new SubdomainTenantResolver(lookup, "example.invalid");
    assertThat(resolver.resolve(request)).contains(TENANT);
    request.setServerName("example.invalid");
    assertThat(resolver.resolve(request)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "evil-example.invalid",
        "synthetic-community.example.invalid.evil",
        "nested.synthetic-community.example.invalid",
        "unknown.example.invalid",
        "localhost"
      })
  void unexpectedProductionHostsFailClosed(String host) {
    var request = new MockHttpServletRequest();
    request.setServerName(host);
    assertThatThrownBy(
            () -> new SubdomainTenantResolver(lookup, "example.invalid").resolve(request))
        .isInstanceOf(TenantResolutionException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"", " ", "https://example.invalid", "example.invalid:8080", ".example.invalid"})
  void productionRequiresAnExplicitValidBaseDomain(String domain) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SubdomainTenantResolver(lookup, domain));
  }
}
