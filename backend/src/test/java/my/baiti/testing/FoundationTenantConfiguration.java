package my.baiti.testing;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class FoundationTenantConfiguration {

  public static final UUID TENANT = UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805");

  @Bean
  HibernatePropertiesCustomizer foundationTenantIdentifier() {
    return properties ->
        properties.put(
            "hibernate.tenant_identifier_resolver",
            new CurrentTenantIdentifierResolver<UUID>() {
              @Override
              public UUID resolveCurrentTenantIdentifier() {
                return TENANT;
              }

              @Override
              public boolean validateExistingCurrentSessions() {
                return true;
              }
            });
  }
}
