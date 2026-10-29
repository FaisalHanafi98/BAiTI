package my.baiti.tenancy;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public final class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

  private static final UUID UNSCOPED = new UUID(0, 0);

  @Override
  public UUID resolveCurrentTenantIdentifier() {
    // Hibernate needs an identifier at startup; an empty DB context still denies all domain rows.
    return TenantContext.current().orElse(UNSCOPED);
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
