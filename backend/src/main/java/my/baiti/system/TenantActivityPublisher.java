package my.baiti.system;

import java.time.Clock;
import java.util.UUID;
import my.baiti.tenancy.TenantContext;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class TenantActivityPublisher {
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public TenantActivityPublisher(ApplicationEventPublisher events, Clock clock) {
    this.events = events;
    this.clock = clock;
  }

  public void record(TenantActivity.Action action, UUID actor, String address) {
    TenantContext.current()
        .ifPresent(
            tenant -> {
              try {
                events.publishEvent(
                    new TenantActivity(tenant, actor, action, address, clock.instant()));
              } catch (RuntimeException failure) {
                LoggerFactory.getLogger(TenantActivityPublisher.class)
                    .error(
                        "Could not enqueue activity {} for tenant {} ({})",
                        action,
                        tenant,
                        failure.getClass().getSimpleName());
              }
            });
  }
}
