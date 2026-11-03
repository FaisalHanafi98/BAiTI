package my.baiti.tenancy;

import my.baiti.people.PermissionSynchronizer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "baiti.security.permission-sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PermissionSyncStartup implements ApplicationRunner {

  private final TenantCatalog tenants;
  private final PermissionSynchronizer synchronizer;

  public PermissionSyncStartup(TenantCatalog tenants, PermissionSynchronizer synchronizer) {
    this.tenants = tenants;
    this.synchronizer = synchronizer;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (TenantContext.current().isPresent()) {
      throw new IllegalStateException("Startup permission sync requires an unscoped caller");
    }
    for (var tenant : tenants.listTenantIds()) {
      TenantContext.set(tenant);
      try {
        synchronizer.synchronizeCurrentTenant();
      } finally {
        TenantContext.clear();
      }
    }
  }
}
