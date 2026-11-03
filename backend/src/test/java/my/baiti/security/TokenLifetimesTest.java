package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenLifetimesTest {
  @Test
  void shorterWholeSecondLifetimesAreSupported() {
    assertThat(new TokenLifetimes(Duration.ofMinutes(5), Duration.ofDays(1)).access())
        .isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void nonpositiveFractionalAndOverlongValuesFailClosed() {
    for (Duration invalid :
        List.of(
            Duration.ZERO,
            Duration.ofSeconds(-1),
            Duration.ofMillis(500),
            Duration.ofMinutes(16))) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new TokenLifetimes(invalid, Duration.ofDays(7)));
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TokenLifetimes(Duration.ofMinutes(15), Duration.ofDays(8)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TokenLifetimes(Duration.ofMinutes(15), Duration.ZERO));
  }
}
