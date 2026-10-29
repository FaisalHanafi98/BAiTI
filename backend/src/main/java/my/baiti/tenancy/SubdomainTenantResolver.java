package my.baiti.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public final class SubdomainTenantResolver implements TenantResolver {

  private final TenantLookup lookup;
  private final String baseDomain;

  public SubdomainTenantResolver(
      TenantLookup lookup, @Value("${baiti.tenancy.base-domain}") String baseDomain) {
    this.lookup = lookup;
    this.baseDomain = baseDomain.toLowerCase(Locale.ROOT);
    if (this.baseDomain.length() > 253
        || !this.baseDomain.matches(
            "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+")) {
      throw new IllegalArgumentException("A valid tenant base domain is required");
    }
  }

  @Override
  public Optional<UUID> resolve(HttpServletRequest request) {
    String host = request.getServerName().toLowerCase(Locale.ROOT);
    if (host.equals(baseDomain)) {
      return Optional.empty();
    }
    String suffix = "." + baseDomain;
    if (!host.endsWith(suffix)) {
      throw new TenantResolutionException();
    }
    String slug = host.substring(0, host.length() - suffix.length());
    if (!slug.matches("[a-z0-9-]{3,50}")) {
      throw new TenantResolutionException();
    }
    return Optional.of(lookup.findActiveTenant(slug).orElseThrow(TenantResolutionException::new));
  }
}
