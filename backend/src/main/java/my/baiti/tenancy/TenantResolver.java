package my.baiti.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

public interface TenantResolver {
  Optional<UUID> resolve(HttpServletRequest request);
}
