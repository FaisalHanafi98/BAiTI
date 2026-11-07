package my.baiti.tenancy;

import my.baiti.system.TenantActivity;
import my.baiti.system.TenantActivityWriter;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Profile("!test | audit-test")
public class TenantActivityListener {
  private final TenantActivityWriter writer;

  public TenantActivityListener(TenantActivityWriter writer) {
    this.writer = writer;
  }

  @Async("tenantAuditExecutor")
  @EventListener
  public void onActivity(TenantActivity activity) {
    TenantContext.clear();
    try {
      TenantContext.set(activity.tenantId());
      writer.append(activity);
    } catch (RuntimeException failure) {
      LoggerFactory.getLogger(TenantActivityListener.class)
          .error(
              "Could not write activity {} for tenant {} ({})",
              activity.action(),
              activity.tenantId(),
              failure.getClass().getSimpleName());
    } finally {
      TenantContext.clear();
    }
  }
}
