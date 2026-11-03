package my.baiti.tenancy;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static Optional<UUID> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static void set(UUID tenantId) {
    CURRENT.set(Objects.requireNonNull(tenantId, "tenantId"));
  }

  public static void clear() {
    CURRENT.remove();
  }
}
