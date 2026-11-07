package my.baiti.security;

import java.util.UUID;
import my.baiti.common.ResourceNotFoundException;
import my.baiti.people.ResidentAccess;
import my.baiti.tenancy.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ResidentScopeGuard {

  private final ResidentAccess residents;

  public ResidentScopeGuard(ResidentAccess residents) {
    this.residents = residents;
  }

  public void requireResident(UUID residentId) {
    var principal = principal();
    if (!residents.ownsResident(principal.userId(), residentId)) {
      throw new ResourceNotFoundException();
    }
  }

  public void requireActiveUnit(UUID unitId) {
    var principal = principal();
    if (!residents.hasActiveUnit(principal.userId(), unitId)) {
      throw new ResourceNotFoundException();
    }
  }

  private TenantPrincipal principal() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof TenantPrincipal principal)
        || !TenantContext.current().filter(principal.tenantId()::equals).isPresent()) {
      throw new ResourceNotFoundException();
    }
    return principal;
  }
}
