package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import my.baiti.people.PermissionSynchronizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PermissionSyncStartupTest {

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void startupBindsEachDiscoveredTenantAndClearsItsContext() {
    var tenants = List.of(UUID.randomUUID(), UUID.randomUUID());
    var observed = new ArrayList<UUID>();
    new PermissionSyncStartup(
            () -> tenants, () -> observed.add(TenantContext.current().orElseThrow()))
        .run(null);
    assertThat(observed).containsExactlyElementsOf(tenants);
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void startupFailureClearsContextAndDoesNotContinueWithAnotherTenant() {
    var tenants = List.of(UUID.randomUUID(), UUID.randomUUID());
    var observed = new ArrayList<UUID>();
    var startup =
        new PermissionSyncStartup(
            () -> tenants,
            () -> {
              observed.add(TenantContext.current().orElseThrow());
              throw new IllegalStateException("Synthetic sync failure");
            });
    assertThatThrownBy(() -> startup.run(null)).hasMessage("Synthetic sync failure");
    assertThat(observed).containsExactly(tenants.get(0));
    assertThat(TenantContext.current()).isEmpty();
  }

  @Test
  void startupCannotOverwriteAnExistingCallerScope() {
    var caller = UUID.randomUUID();
    TenantContext.set(caller);
    var observed = new ArrayList<UUID>();
    var startup =
        new PermissionSyncStartup(
            () -> List.of(UUID.randomUUID()),
            () -> observed.add(TenantContext.current().orElseThrow()));
    assertThatThrownBy(() -> startup.run(null)).isInstanceOf(IllegalStateException.class);
    assertThat(observed).isEmpty();
    assertThat(TenantContext.current()).contains(caller);
  }

  @Test
  void automaticSyncIsEnabledByDefaultAndCanBeDisabledForOwnedTestFixtures() {
    var runner =
        new ApplicationContextRunner()
            .withUserConfiguration(PermissionSyncStartup.class)
            .withBean(TenantCatalog.class, () -> () -> List.of())
            .withBean(PermissionSynchronizer.class, () -> () -> {});
    runner.run(context -> assertThat(context).hasSingleBean(PermissionSyncStartup.class));
    runner
        .withPropertyValues("baiti.security.permission-sync.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(PermissionSyncStartup.class));
  }
}
