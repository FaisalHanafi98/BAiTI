package my.baiti.security;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TenantPrincipal(
    UUID userId, UUID tenantId, Set<String> roles, Set<String> permissions) implements Principal {

  public TenantPrincipal {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(tenantId, "tenantId");
    roles = Set.copyOf(roles);
    permissions = Set.copyOf(permissions);
  }

  @Override
  public String getName() {
    return userId.toString();
  }
}
