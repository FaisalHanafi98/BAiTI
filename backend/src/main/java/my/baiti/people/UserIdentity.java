package my.baiti.people;

import java.util.Set;
import java.util.UUID;

public record UserIdentity(
    UUID userId,
    UUID tenantId,
    String name,
    String email,
    String role,
    Set<String> permissions,
    boolean mustChangePassword) {
  public UserIdentity {
    permissions = Set.copyOf(permissions);
  }
}
