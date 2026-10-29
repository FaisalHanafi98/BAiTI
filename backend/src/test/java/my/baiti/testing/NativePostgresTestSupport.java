package my.baiti.testing;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class NativePostgresTestSupport {

  private static final String JWT_TEST_SECRET = createJwtTestSecret();

  protected static String jwtTestSecret() {
    return JWT_TEST_SECRET;
  }

  private static String createJwtTestSecret() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @DynamicPropertySource
  static void registerNativeDatabase(DynamicPropertyRegistry registry) {
    var config = NativePostgresConfig.from(System.getenv());
    NativePostgresPreflight.verify(config);
    config.register(registry);
    registry.add("baiti.security.jwt.secret", NativePostgresTestSupport::jwtTestSecret);
  }
}
