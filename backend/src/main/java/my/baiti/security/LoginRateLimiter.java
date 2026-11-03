package my.baiti.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class LoginRateLimiter {

  private static final long IDLE_NANOS = Duration.ofMinutes(5).toNanos();
  private final Map<String, Entry> entries = new HashMap<>();
  private final int loginLimit;
  private final int refreshLimit;
  private final int maxKeys;
  private final TimeMeter time;
  private long nextSweep;

  @Autowired
  public LoginRateLimiter(
      @Value("${baiti.security.login-per-minute:5}") int loginLimit,
      @Value("${baiti.security.refresh-per-minute:30}") int refreshLimit) {
    this(loginLimit, refreshLimit, 20_000, TimeMeter.SYSTEM_NANOTIME);
  }

  LoginRateLimiter(int loginLimit, int refreshLimit, int maxKeys, TimeMeter time) {
    if (loginLimit < 1 || refreshLimit < 1 || maxKeys < 1) {
      throw new IllegalArgumentException("Positive authentication limits are required");
    }
    this.loginLimit = loginLimit;
    this.refreshLimit = refreshLimit;
    this.maxKeys = maxKeys;
    this.time = time;
    nextSweep = time.currentTimeNanos();
  }

  public boolean allowIp(String address, boolean refresh) {
    return take((refresh ? "refresh:" : "login:") + address, refresh ? refreshLimit : loginLimit);
  }

  public boolean allowAccount(UUID tenant, String email) {
    String key;
    try {
      key =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(
                          (tenant + ":" + email.toLowerCase(Locale.ROOT))
                              .getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("SHA-256 is required");
    }
    return take("account:" + key, loginLimit);
  }

  private synchronized boolean take(String key, int capacity) {
    long now = time.currentTimeNanos();
    if (now - nextSweep >= 0) {
      entries.entrySet().removeIf(entry -> now - entry.getValue().lastUsed >= IDLE_NANOS);
      nextSweep = now + Duration.ofMinutes(1).toNanos();
    }
    Entry entry = entries.get(key);
    if (entry == null) {
      if (entries.size() >= maxKeys) {
        return false;
      }
      var bucket =
          Bucket.builder()
              .withCustomTimePrecision(time)
              .addLimit(
                  limit -> limit.capacity(capacity).refillGreedy(capacity, Duration.ofMinutes(1)))
              .build();
      entry = new Entry(bucket, now);
      entries.put(key, entry);
    }
    entry.lastUsed = now;
    return entry.bucket.tryConsume(1);
  }

  private static final class Entry {
    private final Bucket bucket;
    private long lastUsed;

    private Entry(Bucket bucket, long lastUsed) {
      this.bucket = bucket;
      this.lastUsed = lastUsed;
    }
  }
}
