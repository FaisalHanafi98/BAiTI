package my.baiti.property;

import java.util.UUID;
import my.baiti.system.PropertyActivity;
import my.baiti.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
class PropertyAudit {
  private final ApplicationEventPublisher events;
  private final AuditorAware<UUID> auditor;

  PropertyAudit(
      ApplicationEventPublisher events, @Qualifier("auditorProvider") AuditorAware<UUID> auditor) {
    this.events = events;
    this.auditor = auditor;
  }

  void record(PropertyActivity.Action action, UUID resourceId) {
    events.publishEvent(
        new PropertyActivity(
            TenantContext.current().orElseThrow(),
            auditor.getCurrentAuditor().orElseThrow(),
            action,
            resourceId));
  }
}
