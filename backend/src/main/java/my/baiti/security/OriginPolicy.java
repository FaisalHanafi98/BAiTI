package my.baiti.security;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record OriginPolicy(Set<String> origins) {
  public OriginPolicy {
    origins = Set.copyOf(origins);
    if (origins.isEmpty()) {
      throw new IllegalArgumentException("An explicit CORS allowlist is required");
    }
    for (String origin : origins) {
      var uri = URI.create(origin);
      if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || !uri.getPath().isEmpty()
          || uri.getPort() > 65535) {
        throw new IllegalArgumentException("CORS origins must be explicit HTTP origins");
      }
    }
  }

  public static OriginPolicy parse(String value) {
    return new OriginPolicy(
        Arrays.stream(value.split(",", -1))
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet()));
  }
}
