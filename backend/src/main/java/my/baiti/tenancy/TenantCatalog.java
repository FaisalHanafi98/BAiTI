package my.baiti.tenancy;

import java.util.List;
import java.util.UUID;

public interface TenantCatalog {
  List<UUID> listTenantIds();
}
