package my.baiti.config;

import java.util.UUID;
import my.baiti.security.TenantActorAuditor;
import my.baiti.tenancy.TenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public HibernatePropertiesCustomizer tenantIdentifier() {
    return properties ->
        properties.put("hibernate.tenant_identifier_resolver", new TenantIdentifierResolver());
  }

  @Bean
  public AuditorAware<UUID> auditorProvider() {
    return new TenantActorAuditor();
  }
}
