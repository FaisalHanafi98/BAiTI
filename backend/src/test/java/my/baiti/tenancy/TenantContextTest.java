package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

  @AfterEach
  void clear() {
    TenantContext.clear();
  }

  @Test
  void storesTypedTenantAndRemovesItExplicitly() {
    assertThat(TenantContext.current()).isEmpty();
    var tenant = UUID.randomUUID();
    TenantContext.set(tenant);
    assertThat(TenantContext.current()).contains(tenant);
    TenantContext.clear();
    assertThat(TenantContext.current()).isEmpty();
    assertThatNullPointerException().isThrownBy(() -> TenantContext.set(null));
  }

  @Test
  void doesNotPropagateTenantIntoAnAsyncWorker() throws Exception {
    TenantContext.set(UUID.randomUUID());
    var executor = Executors.newSingleThreadExecutor();
    try {
      assertThat(executor.submit(TenantContext::current).get()).isEmpty();
    } finally {
      executor.shutdownNow();
    }
  }
}
