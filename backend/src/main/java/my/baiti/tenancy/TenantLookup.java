package my.baiti.tenancy;

import java.util.Optional;
import java.util.UUID;

public interface TenantLookup {
  Optional<UUID> findActiveTenant(String slug);
}
