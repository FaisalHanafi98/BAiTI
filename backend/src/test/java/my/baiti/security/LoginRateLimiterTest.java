package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

  private final FakeTime time = new FakeTime();

  @Test
  void loginIpBudgetsAreIndependentAndRefillWithoutSleeping() {
    var limits = new LoginRateLimiter(2, 4, 10, time);
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
    assertThat(limits.allowIp("192.0.2.1", false)).isFalse();
    assertThat(limits.allowIp("192.0.2.2", false)).isTrue();
    time.now += Duration.ofMinutes(1).toNanos();
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
  }

  @Test
  void accountBudgetsNormaliseEmailCaseAndRemainTenantScoped() {
    var limits = new LoginRateLimiter(1, 4, 10, time);
    var a = UUID.randomUUID();
    assertThat(limits.allowAccount(a, "Synthetic@example.invalid")).isTrue();
    assertThat(limits.allowAccount(a, "SYNTHETIC@example.invalid")).isFalse();
    assertThat(limits.allowAccount(UUID.randomUUID(), "SYNTHETIC@example.invalid")).isTrue();
  }

  @Test
  void refreshHasItsOwnBudgetWithoutResettingTheLoginBudget() {
    var limits = new LoginRateLimiter(1, 2, 10, time);
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
    assertThat(limits.allowIp("192.0.2.1", false)).isFalse();
    assertThat(limits.allowIp("192.0.2.1", true)).isTrue();
    assertThat(limits.allowIp("192.0.2.1", true)).isTrue();
    assertThat(limits.allowIp("192.0.2.1", true)).isFalse();
  }

  @Test
  void cacheSaturationFailsClosedAndIdleEntriesExpire() {
    var limits = new LoginRateLimiter(2, 4, 1, time);
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
    assertThat(limits.allowIp("192.0.2.2", false)).isFalse();
    assertThat(limits.allowIp("192.0.2.1", false)).isTrue();
    time.now += Duration.ofMinutes(5).toNanos();
    assertThat(limits.allowIp("192.0.2.2", false)).isTrue();
  }

  @Test
  void invalidLimitsFailAtConfigurationTime() {
    assertThatIllegalArgumentException().isThrownBy(() -> new LoginRateLimiter(0, 4, 10, time));
    assertThatIllegalArgumentException().isThrownBy(() -> new LoginRateLimiter(2, 0, 10, time));
    assertThatIllegalArgumentException().isThrownBy(() -> new LoginRateLimiter(2, 4, 0, time));
  }

  private static final class FakeTime implements TimeMeter {
    private long now;

    @Override
    public long currentTimeNanos() {
      return now;
    }

    @Override
    public boolean isWallClockBased() {
      return false;
    }
  }
}
