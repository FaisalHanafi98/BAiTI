package my.baiti.security;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TenantPrincipal(
    UUID userId,
    UUID tenantId,
    Set<String> roles,
    Set<String> permissions,
    boolean mustChangePassword)
    implements Principal {

  public TenantPrincipal(UUID userId, UUID tenantId, Set<String> roles, Set<String> permissions) {
    this(userId, tenantId, roles, permissions, false);
  }

  public TenantPrincipal {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(tenantId, "tenantId");
    roles = Set.copyOf(roles);
    permissions = mustChangePassword ? Set.of() : Set.copyOf(permissions);
  }

  @Override
  public String getName() {
    return userId.toString();
  }
}
