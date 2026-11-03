package my.baiti.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("baiti.security.tokens")
public record TokenLifetimes(
    @DefaultValue("15m") Duration access, @DefaultValue("7d") Duration refresh) {
  public TokenLifetimes {
    if (!valid(access, Duration.ofMinutes(15)) || !valid(refresh, Duration.ofDays(7))) {
      throw new IllegalArgumentException(
          "Token lifetimes must use whole seconds within the 15-minute/7-day limits");
    }
  }

  private static boolean valid(Duration value, Duration maximum) {
    return value != null
        && !value.isNegative()
        && !value.isZero()
        && value.getNano() == 0
        && value.compareTo(maximum) <= 0;
  }
}
