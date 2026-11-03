package my.baiti.security;

import java.util.Optional;
import java.util.UUID;
import my.baiti.tenancy.TenantContext;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantActorAuditor implements AuditorAware<UUID> {

  @Override
  public Optional<UUID> getCurrentAuditor() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof TenantPrincipal principal)
        || !TenantContext.current().filter(principal.tenantId()::equals).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(principal.userId());
  }
}
