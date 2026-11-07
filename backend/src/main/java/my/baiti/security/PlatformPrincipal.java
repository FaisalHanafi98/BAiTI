package my.baiti.security;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record PlatformPrincipal(UUID adminId) implements Principal {
  public static final String AUTHORITY = "PLATFORM_ADMIN";

  public PlatformPrincipal {
    Objects.requireNonNull(adminId, "adminId");
  }

  @Override
  public String getName() {
    return adminId.toString();
  }
}
