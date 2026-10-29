package my.baiti.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public final class HeaderTenantResolver implements TenantResolver {

  private final TenantLookup lookup;

  public HeaderTenantResolver(TenantLookup lookup) {
    this.lookup = lookup;
  }

  @Override
  public Optional<UUID> resolve(HttpServletRequest request) {
    List<String> headers = Collections.list(request.getHeaders("X-Tenant-Slug"));
    if (headers.isEmpty()) {
      return Optional.empty();
    }
    if (headers.size() != 1 || !headers.get(0).matches("[a-z0-9-]{3,50}")) {
      throw new TenantResolutionException();
    }
    return Optional.of(
        lookup.findActiveTenant(headers.get(0)).orElseThrow(TenantResolutionException::new));
  }
}
